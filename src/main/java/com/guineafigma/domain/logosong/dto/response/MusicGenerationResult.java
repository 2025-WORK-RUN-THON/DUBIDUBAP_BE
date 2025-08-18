package com.guineafigma.domain.logosong.dto.response;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.request.SunoCallbackRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerationResult {

    private String taskId;
    private MusicGenerationStatus status;
    private String audioUrl;
    private String videoUrl;
    private String imageUrl;
    private Double duration;
    private String errorMessage;
    private String errorType;

    public static MusicGenerationResult fromSunoStatus(SunoStatusResponse response) {
        MusicGenerationStatus status;
        if (response.isCompleted()) {
            status = MusicGenerationStatus.COMPLETED;
        } else if (response.isFailed()) {
            status = MusicGenerationStatus.FAILED;
        } else if (response.isProcessing()) {
            status = MusicGenerationStatus.PROCESSING;
        } else {
            status = MusicGenerationStatus.PENDING;
        }

        return MusicGenerationResult.builder()
                .taskId(response.getId())
                .status(status)
                .audioUrl(response.getAudioUrl())
                .videoUrl(response.getVideoUrl())
                .imageUrl(response.getImageUrl())
                .duration(response.getDuration())
                .errorMessage(response.getErrorMessage())
                .errorType(response.getErrorType())
                .build();
    }

    public static MusicGenerationResult fromCallback(SunoCallbackRequest request) {
        MusicGenerationStatus status;
        if ("complete".equals(request.getStatus())) {
            status = MusicGenerationStatus.COMPLETED;
        } else if ("error".equals(request.getStatus()) || request.getErrorType() != null) {
            status = MusicGenerationStatus.FAILED;
        } else {
            status = MusicGenerationStatus.PROCESSING;
        }

        return MusicGenerationResult.builder()
                .taskId(request.getId())
                .status(status)
                .audioUrl(request.getAudioUrl())
                .videoUrl(request.getVideoUrl())
                .imageUrl(request.getImageUrl())
                .duration(request.getDuration())
                .errorMessage(request.getErrorMessage())
                .errorType(request.getErrorType())
                .build();
    }
}