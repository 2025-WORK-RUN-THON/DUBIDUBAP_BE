package com.guineafigma.domain.chatroom.service;

import com.guineafigma.domain.chatroom.dto.Message;
import com.guineafigma.domain.chatroom.dto.response.OpenAIResponse;
import com.guineafigma.common.converter.TypeConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Component
public class MessageFactory {

    // 사용자 메시지 데이터 생성
    public HashMap<String, Object> createUserMessageData(String content, MultipartFile image) {
        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("content", content);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("type", "user");
        
        // 이미지 처리
        if (image != null && !image.isEmpty()) {
            messageData.put("hasImage", true);
            messageData.put("imageName", image.getOriginalFilename());
        }
        
        return messageData;
    }

    // AI 어시스턴트 메시지 데이터 생성
    public HashMap<String, Object> createAssistantMessageData(OpenAIResponse aiResponse) {
        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("content", aiResponse.getContent());
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("type", "assistant");
        
        return messageData;
    }

    // 초기 메시지 목록 생성 (사용자 메시지 + AI 응답)
    public HashMap<String, Object> createInitialMessages(String userMessage, MultipartFile image, OpenAIResponse aiResponse) {
        HashMap<String, Object> messages = new HashMap<>();
        
        HashMap<String, Object> userMessageData = createUserMessageData(userMessage, image);
        HashMap<String, Object> assistantMessageData = createAssistantMessageData(aiResponse);
        
        messages.put("msg_001", userMessageData);
        messages.put("msg_002", assistantMessageData);
        
        return messages;
    }

    // 사용자 메시지 DTO 생성 (이미지 첨부파일 없음)
    public Message createUserMessageDto(String messageId, String content) {
        return Message.builder()
                .id(messageId)
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 사용자 메시지 DTO 생성 (이미지 첨부파일 포함)
    public Message createUserMessageDtoWithAttachment(String messageId, String content, Long attachmentId, String fileUrl, String originalFilename) {
        Message userMessage = Message.builder()
                .id(messageId)
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        
        Message.AttachmentDto attachment = Message.AttachmentDto.builder()
                .id(attachmentId)
                .fileUrl(fileUrl)
                .originalFilename(originalFilename)
                .build();
        
        userMessage.setAttachments(List.of(attachment));
        return userMessage;
    }

    // AI 어시스턴트 메시지 DTO 생성
    public Message createAssistantMessageDto(String messageId, OpenAIResponse aiResponse) {
        return Message.builder()
                .id(messageId)
                .role("assistant")
                .content(aiResponse.getContent())
                .timestamp(LocalDateTime.now().plusSeconds(15))
                .build();
    }

    // 다음 메시지 ID 생성
    public String generateNextMessageId(HashMap<String, Object> messages) {
        int maxNumber = 0;
        for (String key : messages.keySet()) {
            if (key.startsWith("msg_")) {
                try {
                    int number = Integer.parseInt(key.substring(4));
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException e) {
                    // 잘못된 형식의 키는 무시
                }
            }
        }
        return String.format("msg_%03d", maxNumber + 1);
    }
    
    // 메시지에 이미지 정보 추가 
    public void addImageToMessage(HashMap<String, Object> messageData, String imageUrl, String fileName) {
        messageData.put("hasImage", true);
        messageData.put("imageName", fileName);
        messageData.put("imageUrl", imageUrl);
    }

    // 저장된 메시지들을 Message DTO 리스트로 변환
    public List<Message> convertToMessageList(HashMap<String, Object> messages) {
        return messages.entrySet().stream()
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())) // msg_001, msg_002... 순서
                .map(entry -> {
                    String messageId = entry.getKey();
                    
                    HashMap<String, Object> messageData = TypeConverter.castToHashMap(entry.getValue());
                    
                    String content = (String) messageData.get("content");
                    String type = (String) messageData.get("type");
                    Long timestamp = (Long) messageData.get("timestamp");
                    
                    Message.MessageBuilder builder = Message.builder()
                            .id(messageId)
                            .role(type)
                            .content(content)
                            .timestamp(java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(timestamp != null ? timestamp : System.currentTimeMillis()),
                                    java.time.ZoneId.systemDefault()));
                    
                    // 이미지 첨부파일이 있는 경우
                    Boolean hasImage = (Boolean) messageData.get("hasImage");
                    if (Boolean.TRUE.equals(hasImage)) {
                        String imageUrl = (String) messageData.get("imageUrl");
                        String imageName = (String) messageData.get("imageName");
                        
                        Message.AttachmentDto attachment = Message.AttachmentDto.builder()
                                .id(1L)
                                .fileUrl(imageUrl)
                                .originalFilename(imageName)
                                .build();
                        
                        builder.attachments(List.of(attachment));
                    }
                    
                    return builder.build();
                })
                .collect(java.util.stream.Collectors.toList());
    }
}