package com.guineafigma.domain.media.dto.response;

import com.guineafigma.domain.media.entity.Media;

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
@Schema(description = "미디어 조회 응답")
public class MediaResponse {
    
    @Schema(description = "ID", example = "1")
    private Long id;
    
    @Schema(description = "파일 URL", example = "https://presigned-url...")
    private String fileUrl;
    
    @Schema(description = "원본 파일명", example = "photo.jpg")
    private String originalFilename;
    
    @Schema(description = "가로", example = "1920")
    private Long width;
    
    @Schema(description = "세로", example = "1080")
    private Long height;
    
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;
    
    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "S3 저장 키")
    private String s3Key;

    @Schema(description = "연결된 게시글(카드) ID")
    private Long postId;
    
    public static MediaResponse from(Media media, String fileUrl) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileUrl(fileUrl)
                .originalFilename(media.getOriginalFilename())
                .width(media.getWidth())
                .height(media.getHeight())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .userId(media.getUserId())
                .s3Key(media.getS3Key())
                .postId(media.getPostId())
                .build();
    }
}


