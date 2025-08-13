package com.guineafigma.domain.chatroom.service;

import com.guineafigma.domain.chatroom.dto.ChatRoomMetadata;
import com.guineafigma.domain.chatroom.dto.response.OpenAIResponse;
import com.guineafigma.common.converter.TypeConverter;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class ChatRoomMetadataService {

    // 메타데이터 생성
    public ChatRoomMetadata createMetadata(OpenAIResponse aiResponse, String lastMessageId, int messageCount) {
        ChatRoomMetadata.Usage usage = ChatRoomMetadata.Usage.builder()
                .promptTokens(aiResponse.getPromptTokens())
                .completionTokens(aiResponse.getCompletionTokens())
                .totalTokens(aiResponse.getTotalTokens())
                .model(aiResponse.getModel())
                .cost(aiResponse.getCost())
                .build();
        
        return ChatRoomMetadata.builder()
                .lastMessageId(lastMessageId)
                .messageCount(messageCount)
                .usage(usage)
                .build();
    }

    // 메타데이터를 HashMap으로 변환
    public HashMap<String, Object> convertToMap(ChatRoomMetadata metadata) {
        HashMap<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("lastMessageId", metadata.getLastMessageId());
        metadataMap.put("messageCount", metadata.getMessageCount());
        metadataMap.put("usage", metadata.getUsage());
        
        return metadataMap;
    }

    // 메타데이터에서 Usage 정보 추출
    public ChatRoomMetadata.Usage extractUsageFromMetadata(HashMap<String, Object> metadata) {
        Object usageObj = metadata.get("usage");
        if (usageObj instanceof ChatRoomMetadata.Usage) {
            return (ChatRoomMetadata.Usage) usageObj;
        } else if (usageObj instanceof HashMap) {
            HashMap<String, Object> usageMap = TypeConverter.castToHashMap(usageObj);
            return ChatRoomMetadata.Usage.builder()
                    .promptTokens((Integer) usageMap.get("promptTokens"))
                    .completionTokens((Integer) usageMap.get("completionTokens"))
                    .totalTokens((Integer) usageMap.get("totalTokens"))
                    .model((String) usageMap.get("model"))
                    .cost((Double) usageMap.get("cost"))
                    .build();
        }
        return null;
    }
}