package com.guineafigma.domain.media.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "다중 미디어 업로드 응답")
public class MultipleMediaUploadResponse {
    
    @Schema(description = "업로드된 미디어 정보")
    private List<MediaResponse> media;
    
    @Schema(description = "업로드 성공 개수")
    private int successCount;
    
    @Schema(description = "업로드 실패 개수")
    private int failureCount;
    
    public static MultipleMediaUploadResponse of(List<MediaResponse> media) {
        return MultipleMediaUploadResponse.builder()
                .media(media)
                .successCount(media.size())
                .failureCount(0)
                .build();
    }
}


