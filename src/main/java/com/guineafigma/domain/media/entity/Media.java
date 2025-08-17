package com.guineafigma.domain.media.entity;

import com.guineafigma.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "미디어 엔티티")
public class Media extends BaseEntity {

    @Column(name = "s3_key", nullable = false, length = 255)
    @Schema(description = "S3 저장 키")
    private String s3Key;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "width")
    private Long width;

    @Column(name = "height")
    private Long height;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "post_id")
    private Long postId;

    @Builder
    public Media(String s3Key, String originalFilename, Long width, Long height, Long userId, Long postId) {
        this.s3Key = s3Key;
        this.originalFilename = originalFilename;
        this.width = width;
        this.height = height;
        this.userId = userId;
        this.postId = postId;
    }

    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void updatePostId(Long postId) {
        this.postId = postId;
    }
}


