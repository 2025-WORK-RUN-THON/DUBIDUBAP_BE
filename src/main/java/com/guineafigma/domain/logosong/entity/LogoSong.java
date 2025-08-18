package com.guineafigma.domain.logosong.entity;

import com.guineafigma.common.entity.BaseEntity;
import com.guineafigma.common.enums.MusicGenre;
import com.guineafigma.common.enums.VersionType;
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

    @Column(name = "logosong_url")
    private String logosongUrl;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "music_genre", nullable = false)
    @NotNull
    private MusicGenre musicGenre;

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
}