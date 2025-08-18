package com.guineafigma.domain.logosong.entity;

import com.guineafigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "logosong_likes")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(LogoSongLikeId.class)
public class LogoSongLike {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "logosong_id")
    private Long logosongId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logosong_id", referencedColumnName = "id", insertable = false, updatable = false)
    private LogoSong logoSong;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}