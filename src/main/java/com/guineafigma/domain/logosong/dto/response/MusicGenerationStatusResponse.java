package com.guineafigma.domain.logosong.dto.response;

import com.guineafigma.common.enums.MusicGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "음악 생성 상태 응답 (웹 클라이언트 폴링용)")
public class MusicGenerationStatusResponse {

    @Schema(description = "로고송 ID")
    private Long logoSongId;

    @Schema(description = "Suno API 작업 ID")
    private String taskId;

    @Schema(description = "음악 생성 상태", example = "PROCESSING")
    private MusicGenerationStatus status;

    @Schema(description = "진행률 (0-100)", example = "75")
    private Integer progress;

    @Schema(description = "상태 메시지", example = "음악 생성 중...")
    private String statusMessage;

    @Schema(description = "생성된 음악 URL (완료 시)")
    private String audioUrl;

    @Schema(description = "생성된 비디오 URL (완료 시)")
    private String videoUrl;

    @Schema(description = "음악 길이 (초, 완료 시)")
    private Double duration;

    @Schema(description = "예상 완료 시간 (초)")
    private Integer estimatedCompletionSeconds;

    @Schema(description = "오류 메시지 (실패 시)")
    private String errorMessage;

    @Schema(description = "음악 생성 시작 시간")
    private LocalDateTime startedAt;

    @Schema(description = "음악 생성 완료 시간")
    private LocalDateTime completedAt;

    @Schema(description = "다음 폴링 권장 간격 (초)", example = "5")
    private Integer nextPollInterval;

    @Schema(description = "폴링 만료 시간 (완료되지 않으면 중단)")
    private LocalDateTime pollExpiresAt;

    public static MusicGenerationStatusResponse processing(Long logoSongId, String taskId, LocalDateTime startedAt) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.PROCESSING)
                .progress(calculateProgress(startedAt))
                .statusMessage("음악 생성 중입니다...")
                .estimatedCompletionSeconds(calculateEstimatedTime(startedAt))
                .startedAt(startedAt)
                .nextPollInterval(5) // 5초 간격
                .pollExpiresAt(startedAt.plusMinutes(30)) // 30분 후 만료
                .build();
    }

    public static MusicGenerationStatusResponse completed(Long logoSongId, String taskId, String audioUrl, 
                                                        String videoUrl, Double duration, LocalDateTime startedAt, 
                                                        LocalDateTime completedAt) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.COMPLETED)
                .progress(100)
                .statusMessage("음악 생성이 완료되었습니다!")
                .audioUrl(audioUrl)
                .videoUrl(videoUrl)
                .duration(duration)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .nextPollInterval(null) // 더 이상 폴링 불필요
                .build();
    }

    public static MusicGenerationStatusResponse failed(Long logoSongId, String taskId, String errorMessage, 
                                                      LocalDateTime startedAt) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.FAILED)
                .progress(0)
                .statusMessage("음악 생성에 실패했습니다.")
                .errorMessage(errorMessage)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .nextPollInterval(null) // 더 이상 폴링 불필요
                .build();
    }

    public static MusicGenerationStatusResponse pending(Long logoSongId) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .status(MusicGenerationStatus.PENDING)
                .progress(0)
                .statusMessage("음악 생성 준비 중입니다...")
                .nextPollInterval(3) // 3초 간격으로 빠르게 확인
                .build();
    }

    private static Integer calculateProgress(LocalDateTime startedAt) {
        if (startedAt == null) return 0;
        
        long minutesElapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).toMinutes();
        // 일반적으로 2-5분 소요되므로 진행률 계산
        int progress = Math.min(90, (int) (minutesElapsed * 20)); // 5분에 100%가 되도록
        return Math.max(10, progress); // 최소 10%
    }

    private static Integer calculateEstimatedTime(LocalDateTime startedAt) {
        if (startedAt == null) return 300; // 기본 5분
        
        long secondsElapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).toSeconds();
        int estimatedTotal = 300; // 5분 예상
        return Math.max(30, estimatedTotal - (int) secondsElapsed); // 최소 30초
    }
}