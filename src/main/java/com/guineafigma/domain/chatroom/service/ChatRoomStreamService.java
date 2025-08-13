
package com.guineafigma.domain.chatroom.service;

import com.guineafigma.common.converter.TypeConverter;
import com.guineafigma.domain.chatroom.dto.ChatRoomMetadata;
import com.guineafigma.domain.chatroom.dto.request.ChatRoomMessageRequest;
import com.guineafigma.domain.chatroom.dto.response.OpenAIResponse;
import com.guineafigma.domain.chatroom.entity.ChatRoom;
import com.guineafigma.domain.chatroom.repository.ChatRoomRepository;
import com.guineafigma.domain.image.dto.response.ImageResponse;
import com.guineafigma.domain.image.enums.ImageType;
import com.guineafigma.domain.image.service.ImageService;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomStreamService {

    private final ChatRoomRepository chatRoomRepository;
    private final OpenAIStreamService openAIStreamService;
    private final OpenAIService openAIService; // 제목 생성을 위해 추가
    private final MessageFactory messageFactory; // 메시지 생성을 위해 추가
    private final ImageService imageService; // 이미지 처리를 위해 추가
    private final ChatRoomMetadataService metadataService; // 메타데이터 처리를 위해 추가
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    private final AtomicLong connectionIdGenerator = new AtomicLong(0);

    @Transactional
    public SseEmitter streamChatRoomMessage(ChatRoomMessageRequest request, Long userId) {
        String connectionId = "conn_" + userId + "_" + connectionIdGenerator.incrementAndGet();
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        try {
            // 채팅방 준비 (조회 또는 생성) 및 사용자 메시지 저장
            ChatRoom chatRoom = prepareChatRoomAndSaveUserMessage(request, userId);

            activeConnections.put(connectionId, emitter);
            emitter.onCompletion(() -> cleanupConnection(connectionId));
            emitter.onTimeout(() -> cleanupConnection(connectionId));
            emitter.onError(error -> cleanupConnection(connectionId));

            log.info("새 SSE 연결 생성: {} (채팅방 ID: {})", connectionId, chatRoom.getId());

            // 스트림 시작 이벤트 전송
            sendStreamStartEvent(emitter, connectionId, request);

            // 메시지 히스토리 구성 및 스트림 처리
            List<Map<String, Object>> messageHistory = buildMessageHistoryForOpenAI(chatRoom);
            Flux<String> streamFlux = openAIStreamService.sendMessageStream(
                    request.getMessage(),
                    request.getImage(),
                    messageHistory
            )
            .timeout(Duration.ofMinutes(3))
            .retry(2);

            processStreamEvents(streamFlux, emitter, connectionId, chatRoom);

        } catch (Exception e) {
            log.error("스트림 처리 중 오류 발생 [{}]: {}", connectionId, e.getMessage());
            sendErrorToClient(emitter, connectionId, "스트림 초기화 실패", e);
        }

        return emitter;
    }

    // 채팅방을 준비하고 사용자의 첫 메시지를 저장하는 메소드
    private ChatRoom prepareChatRoomAndSaveUserMessage(ChatRoomMessageRequest request, Long userId) {
        if (request.getChatRoomId() == null) {
            // 새 채팅방 생성
            return createNewChatRoom(userId, request);
        } else {
            // 기존 채팅방에 메시지 추가
            return addMessageToExistingChatRoom(request.getChatRoomId(), userId, request);
        }
    }

    // 새 채팅방 생성 로직 
    private ChatRoom createNewChatRoom(Long userId, ChatRoomMessageRequest request) {
        String title = openAIService.generateTitle(request.getMessage());

        ChatRoom chatRoom = buildChatRoomWithoutImage(userId, title, request);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            updateImageInfoInMessage(savedChatRoom, request.getImage(), userId);
        }
        return savedChatRoom;
    }
    
    // 기존 채팅방에 메시지 추가 로직 (ChatRoomService 참조)
    private ChatRoom addMessageToExistingChatRoom(Long chatRoomId, Long userId, ChatRoomMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        validateChatRoomOwnership(chatRoom, userId);
        
        addUserMessageToChatRoom(chatRoom, request, userId);
        return chatRoom;
    }

    // 스트림 완료 후 AI 응답을 저장하는 메소드
    @Transactional
    public void saveAssistantResponse(ChatRoom chatRoom, String finalMessage) {
        try {
            // 임시 OpenAIResponse 객체 생성 (토큰 정보 등은 알 수 없으므로 기본값 사용)
            OpenAIResponse aiResponse = OpenAIResponse.builder()
                    .content(finalMessage)
                    .model("gpt-4o-mini") // 스트리밍에 사용된 모델명 기입
                    .promptTokens(0) .completionTokens(0) .totalTokens(0) .cost(0.0)
                    .build();

            addAiResponseToMessages(chatRoom, aiResponse);
            chatRoomRepository.save(chatRoom);
            log.info("AI 응답 저장 완료 - 채팅방 ID: {}", chatRoom.getId());
        } catch (Exception e) {
            log.error("AI 응답 저장 실패 - 채팅방 ID: {}: {}", chatRoom.getId(), e.getMessage(), e);
        }
    }

    // 스트림 이벤트 처리
    private void processStreamEvents(Flux<String> streamFlux, SseEmitter emitter, String connectionId,
            ChatRoom chatRoom) {
        StringBuilder messageBuilder = new StringBuilder();
        AtomicLong chunkCounter = new AtomicLong(0);

        streamFlux
                .filter(line -> line.trim().startsWith("{"))
                .subscribe(
                        line -> {
                            try {
                                String content = parseOpenAIJSON(line, connectionId);
                                if (content != null && !content.isEmpty()) {
                                    messageBuilder.append(content);
                                    long chunkIndex = chunkCounter.incrementAndGet();
                                    sendMessageChunkEvent(emitter, connectionId, content, messageBuilder.toString(),
                                            chunkIndex);
                                }
                            } catch (Exception e) {
                                log.error("SSE 이벤트 전송 오류 [{}]: {}", connectionId, e.getMessage());
                            }
                        },
                        error -> {
                            log.error("스트림 오류 [{}]: {}", connectionId, error.getMessage());
                            sendErrorToClient(emitter, connectionId, "스트림 처리 오류", error);
                        },
                        () -> {
                            try {
                                String finalMessage = messageBuilder.toString();
                                // AI 응답 저장
                                saveAssistantResponse(chatRoom, finalMessage);

                                sendStreamCompleteEvent(emitter, connectionId, finalMessage, chunkCounter.get());
                                emitter.complete();
                                log.info("스트림 정상 완료 [{}]", connectionId);
                            } catch (Exception e) {
                                log.error("스트림 완료 처리 오류 [{}]: {}", connectionId, e.getMessage());
                            }
                        });
    }

    private void updateImageInfoInMessage(ChatRoom chatRoom, MultipartFile image, Long userId) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        String firstMessageId = (String) chatRoom.getMetadata().get("lastMessageId");
        HashMap<String, Object> userMessage = TypeConverter.castToHashMap(messages.get(firstMessageId));

        processImageUpload(userMessage, image, userId, chatRoom.getId());

        messages.put(firstMessageId, userMessage);
        chatRoom.updateMessages(messages);
    }

    private void addUserMessageToChatRoom(ChatRoom chatRoom, ChatRoomMessageRequest request, Long userId) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        String newMessageId = messageFactory.generateNextMessageId(messages);
        HashMap<String, Object> messageData = messageFactory.createUserMessageData(request.getMessage(), null);

        processImageUpload(messageData, request.getImage(), userId, chatRoom.getId());

        messages.put(newMessageId, messageData);

        HashMap<String, Object> metadata = chatRoom.getMetadata();
        metadata.put("lastMessageId", newMessageId);
        metadata.put("messageCount", messages.size());

        chatRoom.updateMessages(messages);
        chatRoom.updateMetadata(metadata);
    }

    private String addAiResponseToMessages(ChatRoom chatRoom, OpenAIResponse aiResponse) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        HashMap<String, Object> assistantMessage = messageFactory.createAssistantMessageData(aiResponse);
        String nextMessageId = messageFactory.generateNextMessageId(messages);
        messages.put(nextMessageId, assistantMessage);

        HashMap<String, Object> metadata = chatRoom.getMetadata();
        ChatRoomMetadata chatRoomMetadata = metadataService.createMetadata(aiResponse, nextMessageId, messages.size());
        HashMap<String, Object> updatedMetadata = metadataService.convertToMap(chatRoomMetadata);
        metadata.putAll(updatedMetadata);

        chatRoom.updateMessages(messages);
        chatRoom.updateMetadata(metadata);
        return nextMessageId;
    }

    private void validateChatRoomOwnership(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private void processImageUpload(HashMap<String, Object> messageData, MultipartFile image, Long userId, Long chatRoomId) {
        if (image != null && !image.isEmpty()) {
            ImageResponse imageResponse = imageService.uploadImageByType(
                ImageType.USER_CHAT, image, userId, chatRoomId);
            messageFactory.addImageToMessage(messageData, imageResponse.getFileUrl(), image.getOriginalFilename());
        }
    }

    private ChatRoom buildChatRoomWithoutImage(Long userId, String title, ChatRoomMessageRequest request) {
        HashMap<String, Object> initialMessages = messageFactory.createUserMessageData(request.getMessage(), null);
        HashMap<String, Object> messages = new HashMap<>();
        String firstMessageId = messageFactory.generateNextMessageId(messages);
        messages.put(firstMessageId, initialMessages);

        HashMap<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("lastMessageId", firstMessageId);
        metadataMap.put("messageCount", 1);

        return ChatRoom.builder()
                .userId(userId)
                .title(title)
                .messages(messages)
                .metadata(metadataMap)
                .build();
    }

    private List<Map<String, Object>> buildMessageHistoryForOpenAI(ChatRoom chatRoom) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        List<String> sortedMessageIds = messages.keySet().stream()
                .filter(key -> key.startsWith("msg_"))
                .sorted().toList();

        int maxMessages = 20;
        int startIndex = Math.max(0, sortedMessageIds.size() - maxMessages);
        List<String> recentMessageIds = sortedMessageIds.subList(startIndex, sortedMessageIds.size());

        List<Map<String, Object>> messageHistory = new ArrayList<>();
        for (String messageId : recentMessageIds) {
            HashMap<String, Object> messageData = TypeConverter.castToHashMap(messages.get(messageId));
            String type = (String) messageData.get("type");
            String content = (String) messageData.get("content");
            messageHistory.add(Map.of("role", "user".equals(type) ? "user" : "assistant", "content", content));
        }
        return messageHistory;
    }

    private String parseOpenAIJSON(String jsonLine, String connectionId) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonLine.trim());
            JsonNode choices = jsonNode.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                if (delta.has("content")) {
                    return delta.get("content").asText();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("OpenAI JSON 파싱 오류 [{}] - JSON: '{}'", connectionId, jsonLine);
            return null;
        }
    }

    private void sendStreamStartEvent(SseEmitter emitter, String connectionId, ChatRoomMessageRequest request) {
        try {
            Map<String, Object> startEvent = Map.of(
                "eventType", "stream_start", "connectionId", connectionId,
                "requestInfo", Map.of("messageLength", request.getMessage().length(), "hasImage", request.getImage() != null),
                "timestamp", System.currentTimeMillis()
            );
            emitter.send(SseEmitter.event().name("stream_start").data(startEvent));
        } catch (Exception e) {
            log.warn("스트림 시작 이벤트 전송 실패 [{}]: {}", connectionId, e.getMessage());
        }
    }

    private void sendMessageChunkEvent(SseEmitter emitter, String connectionId, String chunkContent, String accumulatedMessage, long chunkIndex) {
        try {
            Map<String, Object> chunkEvent = Map.of(
                "eventType", "message_chunk",
                "chunk", Map.of("content", chunkContent, "index", chunkIndex),
                "message", Map.of("accumulated", accumulatedMessage, "characterCount", accumulatedMessage.length()),
                "timestamp", System.currentTimeMillis()
            );
            emitter.send(SseEmitter.event().name("message_chunk").data(chunkEvent));
        } catch (Exception e) {
            // 클라이언트 연결 종료 등으로 인한 오류는 무시
        }
    }

    private void sendStreamCompleteEvent(SseEmitter emitter, String connectionId, String finalMessage, long totalChunks) {
        try {
            Map<String, Object> completeEvent = Map.of(
                "eventType", "stream_complete",
                "finalMessage", Map.of("content", finalMessage, "totalChunks", totalChunks),
                "timestamp", System.currentTimeMillis()
            );
            emitter.send(SseEmitter.event().name("stream_complete").data(completeEvent));
        } catch (Exception e) {
            log.warn("스트림 완료 이벤트 전송 실패 [{}]: {}", connectionId, e.getMessage());
        }
    }

    private void sendErrorToClient(SseEmitter emitter, String connectionId, String errorType, Throwable error) {
        try {
            Map<String, Object> errorEvent = Map.of(
                "eventType", "stream_error",
                "error", Map.of("type", errorType, "message", error.getMessage()),
                "timestamp", System.currentTimeMillis()
            );
            emitter.send(SseEmitter.event().name("stream_error").data(errorEvent));
        } catch (Exception e) {
            log.warn("에러 정보 전송 실패 [{}]: {}", connectionId, e.getMessage());
        } finally {
            emitter.completeWithError(error);
        }
    }

    private void cleanupConnection(String connectionId) {
        if (activeConnections.remove(connectionId) != null) {
            log.info("연결 정리 완료: {} (남은 활성 연결 수: {})", connectionId, activeConnections.size());
        }
    }
}
