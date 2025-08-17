package com.guineafigma.domain.image.entity;

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
@Schema(description = "이미지 엔티티")
public class Image extends BaseEntity {

    @Column(name = "s3_key", nullable = false, length = 255)
    @Schema(description = "S3 저장 키", example = "users/user_abc123/profile/uuid-filename.jpg")
    private String s3Key;

    @Column(name = "original_filename", nullable = false, length = 255)
    @Schema(description = "원본 파일명", example = "diving_photo.jpg")
    private String originalFilename;

    @Column(name = "width")
    @Schema(description = "이미지 너비", example = "1920")
    private Long width;

    @Column(name = "height")
    @Schema(description = "이미지 높이", example = "1080")
    private Long height;

    @Column(name = "user_id")
    @Schema(description = "업로드한 사용자 ID", example = "1")
    private Long userId;

    @Column(name = "post_id")
    @Schema(description = "연결된 게시글 ID", example = "123")
    private Long postId;

    @Builder
    public Image(String s3Key, String originalFilename, Long width, Long height, Long userId, Long postId) {
        this.s3Key = s3Key;
        this.originalFilename = originalFilename;
        this.width = width;
        this.height = height;
        this.userId = userId;
        this.postId = postId;
    }

    // 이미지 정보 업데이트
    public void updateDimensions(Long width, Long height) {
        this.width = width;
        this.height = height;
    }
    

    // S3 키 업데이트
    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    // 게시글 ID 업데이트 (temp → permanent 변환 시)
    public void updatePostId(Long postId) {
        this.postId = postId;
    }
}