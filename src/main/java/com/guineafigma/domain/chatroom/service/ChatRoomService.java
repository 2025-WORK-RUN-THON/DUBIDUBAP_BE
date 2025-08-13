package com.guineafigma.domain.chatroom.service;

import com.guineafigma.domain.chatroom.dto.ChatRoomMetadata;
import com.guineafigma.domain.chatroom.dto.Message;
import com.guineafigma.domain.chatroom.dto.request.ChatRoomMessageRequest;
import com.guineafigma.domain.chatroom.dto.response.ChatRoomDetailResponse;
import com.guineafigma.domain.chatroom.dto.response.ChatRoomMessageResponse;
import com.guineafigma.domain.chatroom.dto.response.ChatRoomResponse;
import com.guineafigma.domain.chatroom.dto.response.OpenAIResponse;
import com.guineafigma.domain.chatroom.entity.ChatRoom;
import com.guineafigma.domain.chatroom.repository.ChatRoomRepository;
import com.guineafigma.domain.image.dto.response.ImageResponse;
import com.guineafigma.domain.image.enums.ImageType;
import com.guineafigma.domain.image.service.ImageService;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import com.guineafigma.common.converter.TypeConverter;

import lombok.RequiredArgsConstructor;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final OpenAIService openAIService;
    private final MessageFactory messageFactory;
    private final ImageService imageService;
    private final ChatRoomMetadataService metadataService;  

    // 채팅방 메시지 전송 (새 채팅방 생성 또는 기존 채팅방에 메시지 추가)
    @Transactional
    public ChatRoomMessageResponse sendChatRoomMessage(ChatRoomMessageRequest request, Long userId) {
        ChatRoom chatRoom;
        List<String> newMessageIds = new java.util.ArrayList<>();
        
        // 기존 채팅방 ID가 오지 않은 경우 
        if (request.getChatRoomId() == null) {
            // 새 채팅방 생성
            chatRoom = createNewChatRoom(userId, request);
        } else {
            // 기존 채팅방에 메시지 추가
            chatRoom = addMessageToExistingChatRoom(request.getChatRoomId(), userId, request);
        }
        // 채팅방 메시지 ID 추가
        HashMap<String, Object> metadata = chatRoom.getMetadata();
        newMessageIds.add((String) metadata.get("lastMessageId"));

        
        // AI 응답 생성
        OpenAIResponse aiResponse;
        if (request.getChatRoomId() == null) {
            // 새 채팅방 - 히스토리 없이 메시지 전달
            aiResponse = openAIService.sendMessageWithHistory(request.getMessage(), request.getImage(), null);
        } else {
            // 기존 채팅방 - 기존 메세지 최대 20개 포함해서 전달
            List<Map<String, Object>> messageHistory = buildMessageHistoryForOpenAI(chatRoom);
            aiResponse = openAIService.sendMessageWithHistory(request.getMessage(), request.getImage(), messageHistory);
        }
        
        // AI 응답을 채팅방에 추가
        String aiMessageId = addAiResponseToMessages(chatRoom, aiResponse);
        newMessageIds.add(aiMessageId);
        
        return buildMessageResponse(chatRoom, newMessageIds);
    }

    // 새 채팅방 생성
    private ChatRoom createNewChatRoom(Long userId, ChatRoomMessageRequest request) {
        String title = openAIService.generateTitle(request.getMessage());

        // 채팅방을 먼저 저장 (이미지 없이)
        ChatRoom chatRoom = buildChatRoomWithoutImage(userId, title, request);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 이미지가 있으면 채팅방 첫 메시지에 이미지 정보 추가
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            HashMap<String, Object> messages = savedChatRoom.getMessages();
            HashMap<String, Object> metadata = savedChatRoom.getMetadata();
            String firstMessageId = (String) metadata.get("lastMessageId");
            HashMap<String, Object> userMessage = TypeConverter.castToHashMap(messages.get(firstMessageId));

            processImageUpload(userMessage, request.getImage(), userId, savedChatRoom.getId());

            messages.put(firstMessageId, userMessage);
            savedChatRoom.updateMessages(messages);
        }

        return savedChatRoom;
    }

    // 기존 채팅방에 메시지 추가
    private ChatRoom addMessageToExistingChatRoom(Long chatRoomId, Long userId, ChatRoomMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        validateChatRoomOwnership(chatRoom, userId);
        
        // 새 메시지 추가
        addUserMessageToChatRoom(chatRoom, request, userId);
        
        return chatRoom;
    }
    
    // 사용자 메시지를 채팅방에 추가
    private void addUserMessageToChatRoom(ChatRoom chatRoom, ChatRoomMessageRequest request, Long userId) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        String newMessageId = messageFactory.generateNextMessageId(messages);

        // 메시지 데이터 생성
        HashMap<String, Object> messageData = messageFactory.createUserMessageData(request.getMessage(), null);

        // 이미지 처리
        processImageUpload(messageData, request.getImage(), userId, chatRoom.getId());

        messages.put(newMessageId, messageData);

        // 메타데이터 업데이트
        HashMap<String, Object> metadata = chatRoom.getMetadata();
        metadata.put("lastMessageId", newMessageId);
        metadata.put("messageCount", messages.size());

        chatRoom.updateMessages(messages);
        chatRoom.updateMetadata(metadata);
    }
    
    // AI 응답을 채팅방 메시지에 추가
    private String addAiResponseToMessages(ChatRoom chatRoom, OpenAIResponse aiResponse) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        HashMap<String, Object> assistantMessage = messageFactory.createAssistantMessageData(aiResponse);
        
        // 다음 메시지 ID 생성
        String nextMessageId = messageFactory.generateNextMessageId(messages);
        messages.put(nextMessageId, assistantMessage);
        
        // 기존 메타데이터 가져와서 업데이트
        HashMap<String, Object> metadata = chatRoom.getMetadata();
        metadata.put("lastMessageId", nextMessageId);
        metadata.put("messageCount", messages.size());
        
        // 메타데이터 업데이트
        ChatRoomMetadata chatRoomMetadata = metadataService.createMetadata(aiResponse, nextMessageId, messages.size());
        HashMap<String, Object> updatedMetadata = metadataService.convertToMap(chatRoomMetadata);
        metadata.putAll(updatedMetadata);
        
        chatRoom.updateMessages(messages);
        chatRoom.updateMetadata(metadata);
        
        return nextMessageId;
    }
    
    // 채팅방 소유자 권한 확인
    private void validateChatRoomOwnership(ChatRoom chatRoom, Long userId) {
        if (!chatRoom.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
    
    
    // 이미지 업로드 처리
    private void processImageUpload(HashMap<String, Object> messageData, MultipartFile image, Long userId, Long chatRoomId) {
        if (image != null && !image.isEmpty()) {
            ImageResponse imageResponse = imageService.uploadImageByType(
                ImageType.USER_CHAT,
                image,
                userId,
                chatRoomId
            );
            String imageUrl = imageResponse.getFileUrl();
            String fileName = image.getOriginalFilename();
            
            messageFactory.addImageToMessage(messageData, imageUrl, fileName);
        }
    }

    /**
     * ChatRoom 엔티티 생성 (이미지 없이)
    * 첫 생성때는 chatRoomId가 없기 때문 (이미지 업로드 시 chatRoomId 필요)
     */
    private ChatRoom buildChatRoomWithoutImage(Long userId, String title, ChatRoomMessageRequest request) {
        // 이미지 없이 초기 메시지만 생성
        HashMap<String, Object> initialMessages = messageFactory.createUserMessageData(request.getMessage(), null);
        HashMap<String, Object> messages = new HashMap<>();
        String firstMessageId = messageFactory.generateNextMessageId(messages);
        messages.put(firstMessageId, initialMessages);
        
        // 임시 메타데이터 (AI 응답 전)
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


    // 응답 DTO 생성
    private ChatRoomDetailResponse buildDetailResponse(ChatRoom savedChatRoom) {
        ChatRoomResponse chatRoomResponse = ChatRoomResponse.from(savedChatRoom);
        
        // 전체 메시지 변환
        List<Message> messages = messageFactory.convertToMessageList(savedChatRoom.getMessages());
        
        // 메타데이터에서 사용량 정보 가져오기
        ChatRoomMetadata.Usage usage = metadataService.extractUsageFromMetadata(savedChatRoom.getMetadata());
        
        return ChatRoomDetailResponse.builder()
                .chatRoom(chatRoomResponse)
                .messages(messages)
                .usage(usage)
                .build();
    }

    // 사용자별 채팅방 목록 조회
    public List<ChatRoomResponse> getChatRoomsByUserId(Long userId) {
            List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId);

            return chatRooms.stream()
                            .map(ChatRoomResponse::from)
                            .collect(Collectors.toList());
    }
    
    public ChatRoomDetailResponse getChatRoomDetail(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return buildDetailResponse(chatRoom);
    }
    
    // OpenAI API용 메시지 히스토리 구성 (최대 20개 메시지)
    private List<Map<String, Object>> buildMessageHistoryForOpenAI(ChatRoom chatRoom) {
        HashMap<String, Object> messages = chatRoom.getMessages();
        
        // 메시지 ID로 정렬 (msg_001, msg_002, ...)
        List<String> sortedMessageIds = messages.keySet().stream()
                .filter(key -> key.startsWith("msg_"))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        // 최근 20개 메시지만 선택 (현재 사용자 메시지 제외)
        int maxMessages = 20;
        int startIndex = Math.max(0, sortedMessageIds.size() - maxMessages);
        List<String> recentMessageIds = sortedMessageIds.subList(startIndex, sortedMessageIds.size());
        
        List<Map<String, Object>> messageHistory = new java.util.ArrayList<>();
        
        for (String messageId : recentMessageIds) {
            HashMap<String, Object> messageData = TypeConverter.castToHashMap(messages.get(messageId));
            
            String type = (String) messageData.get("type");
            String content = (String) messageData.get("content");
            
            Map<String, Object> openAIMessage = new HashMap<>();
            openAIMessage.put("role", "user".equals(type) ? "user" : "assistant");
            openAIMessage.put("content", content);
            
            messageHistory.add(openAIMessage);
        }
        
        return messageHistory;
    }
    
    // 새 메시지만 포함한 응답 생성
    private ChatRoomMessageResponse buildMessageResponse(ChatRoom chatRoom, List<String> newMessageIds) {
        // 새 메시지들만 변환
        HashMap<String, Object> allMessages = chatRoom.getMessages();
        HashMap<String, Object> newMessagesMap = new HashMap<>();
        
        for (String messageId : newMessageIds) {
            newMessagesMap.put(messageId, allMessages.get(messageId));
        }
        
        List<Message> newMessages = messageFactory.convertToMessageList(newMessagesMap);
        
        // 메타데이터에서 사용량 정보 가져오기
        ChatRoomMetadata.Usage usage = metadataService.extractUsageFromMetadata(chatRoom.getMetadata());
        
        return ChatRoomMessageResponse.builder()
                .chatRoomId(chatRoom.getId())
                .title(chatRoom.getTitle())
                .newMessages(newMessages)
                .usage(usage)
                .build();
    }
    
    // 채팅방 삭제
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        validateChatRoomOwnership(chatRoom, userId);
        
        chatRoomRepository.delete(chatRoom);
    }
    
    // 채팅방 제목 변경
    @Transactional
    public void updateChatRoomTitle(Long chatRoomId, Long userId, String title) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        validateChatRoomOwnership(chatRoom, userId);
        
        chatRoom.updateTitle(title);
    }
} 