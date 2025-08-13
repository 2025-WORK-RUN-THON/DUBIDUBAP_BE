package com.guineafigma.domain.image.dto.response;

import com.guineafigma.domain.image.entity.Image;
import com.guineafigma.domain.image.enums.ImageType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이미지 조회 응답")
public class ImageResponse {
    
    @Schema(description = "이미지 ID", example = "1")
    private Long id;
    
    @Schema(description = "이미지 파일 URL", example = "https://presigned-url...")
    private String fileUrl;
    
    @Schema(description = "원본 파일명", example = "diving_photo.jpg")
    private String originalFilename;
    
    @Schema(description = "이미지 너비", example = "1920")
    private Long width;
    
    @Schema(description = "이미지 높이", example = "1080")
    private Long height;
    
    @Schema(description = "이미지 타입", example = "USER_CHAT")
    private ImageType type;
    
    @Schema(description = "생성 일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2025-01-15T10:30:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "S3 저장 키", example = "system/dogam_profile/1/filename.jpg")
    private String s3Key;

    @Schema(description = "연결된 게시글(카드) ID", example = "123")
    private Long postId;
    
    public static ImageResponse from(Image image, String fileUrl) {
        return ImageResponse.builder()
                .id(image.getId())
                .fileUrl(fileUrl)
                .originalFilename(image.getOriginalFilename())
                .width(image.getWidth())
                .height(image.getHeight())
                .type(image.getType())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .userId(image.getUserId())
                .s3Key(image.getS3Key())
                .postId(image.getPostId())
                .build();
    }
} 