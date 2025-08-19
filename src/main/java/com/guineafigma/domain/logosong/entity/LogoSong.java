package com.guineafigma.domain.logosong.entity;

import com.guineafigma.common.entity.BaseEntity;
import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.common.enums.VersionType;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "logosongs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoSong extends BaseEntity {

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "service_name", nullable = false)
    @NotNull
    private String serviceName;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "industry")
    private String industry;

    @Column(name = "marketing_item")
    private String marketingItem;

    @Column(name = "target_customer")
    private String targetCustomer;

    @Column(name = "mood_tone")
    private String moodTone;

    @Column(name = "music_genre", nullable = false)
    @NotNull
    private String musicGenre;

    @Enumerated(EnumType.STRING)
    @Column(name = "version", nullable = false)
    @NotNull
    private VersionType version;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "lyrics", columnDefinition = "TEXT")
    private String lyrics;

    @Column(name = "video_guideline", columnDefinition = "TEXT")
    private String videoGuideline;

    @Column(name = "suno_task_id")
    private String sunoTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "music_status")
    private MusicGenerationStatus musicStatus;

    @Column(name = "generated_music_url")
    private String generatedMusicUrl;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public void updateVideoGuideline(String videoGuideline) {
        this.videoGuideline = videoGuideline;
    }

    public void updateSunoTaskId(String sunoTaskId) {
        this.sunoTaskId = sunoTaskId;
    }

    public void updateMusicStatus(MusicGenerationStatus status) {
        this.musicStatus = status;
        if (status == MusicGenerationStatus.COMPLETED) {
            this.generatedAt = LocalDateTime.now();
        }
    }

    public void updateGeneratedMusicUrl(String url) {
        this.generatedMusicUrl = url;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}