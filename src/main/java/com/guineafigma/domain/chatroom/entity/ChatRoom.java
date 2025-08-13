package com.guineafigma.domain.chatroom.entity;

import com.guineafigma.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅방 엔티티")
public class ChatRoom extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Column(name = "title", nullable = false, length = 20)
    @Schema(description = "채팅방 제목", example = "해양생물 문의")
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "JSON")
    @Schema(description = "채팅 메시지들 (JSON 형태)")
    private HashMap<String, Object> messages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    @Schema(description = "채팅방 메타데이터 (JSON 형태)")
    private HashMap<String, Object> metadata;

    @Builder
    public ChatRoom(Long userId, String title, HashMap<String, Object> messages, HashMap<String, Object> metadata) {
        this.userId = userId;
        this.title = title;
        this.messages = messages;
        this.metadata = metadata;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateMessages(HashMap<String, Object> messages) {
        this.messages = messages;
    }

    public void updateMetadata(HashMap<String, Object> metadata) {
        this.metadata = metadata;
    }
}