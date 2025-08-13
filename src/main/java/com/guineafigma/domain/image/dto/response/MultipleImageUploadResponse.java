package com.guineafigma.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "다중 이미지 업로드 응답")
public class MultipleImageUploadResponse {
    
    @Schema(description = "업로드된 이미지들의 정보")
    private List<ImageResponse> images;
    
    @Schema(description = "업로드 성공한 이미지 개수")
    private int successCount;
    
    @Schema(description = "업로드 실패한 이미지 개수")
    private int failureCount;
    
    public static MultipleImageUploadResponse of(List<ImageResponse> images) {
        return MultipleImageUploadResponse.builder()
                .images(images)
                .successCount(images.size())
                .failureCount(0)
                .build();
    }
}