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

    @Schema(description = "음악 생성 대상 로고송의 고유 식별자", example = "123", required = true, nullable = false)
    private Long logoSongId;

    @Schema(description = "Suno AI 음악 생성 작업의 고유 식별자", example = "550e8400-e29b-41d4-a716-446655440000", required = false, nullable = true)
    private String taskId;

    @Schema(description = "음악 생성 상태", example = "PROCESSING", required = false, nullable = true)
    private MusicGenerationStatus status;

    @Schema(description = "음악 생성 진행률 (0-100% 백분율)", example = "75", required = false, nullable = true)
    private Integer progress;

    @Schema(description = "사용자에게 노출할 상태 메시지", example = "음악 생성 중입니다...", required = false, nullable = true)
    private String statusMessage;

    @Schema(description = "생성 완료된 음악 파일 다운로드 URL (상태가 COMPLETED일 때만 제공)", example = "https://cdn1.suno.ai/550e8400-e29b-41d4-a716-446655440000.mp3", required = false, nullable = true)
    private String audioUrl;

    @Schema(description = "로고송 대표 이미지 URL", example = "https://cdn.example.com/images/cafe-logo-123.jpg", required = false, nullable = true)
    private String imageUrl;

    @Schema(description = "생성 완료된 비디오 파일 URL (옵션얼, Suno에서 제공시)", example = "https://cdn1.suno.ai/550e8400-e29b-41d4-a716-446655440000.mp4", required = false, nullable = true)
    private String videoUrl;

    @Schema(description = "생성된 음악의 재생 시간 (초 단위, 완료 시만 제공)", example = "25.5", required = false, nullable = true)
    private Double duration;

    @Schema(description = "예상 완료까지 남은 시간 (초 단위, 진행 중일 때만 제공)", example = "120", required = false, nullable = true)
    private Integer estimatedCompletionSeconds;

    @Schema(description = "음악 생성 실패 시 상세 오류 메시지 (상태가 FAILED일 때만 제공)", example = "Content violates usage policy", required = false, nullable = true)
    private String errorMessage;

    @Schema(description = "음악 생성 작업이 시작된 일시", example = "2025-08-19T14:30:00", required = false, nullable = true)
    private LocalDateTime startedAt;

    @Schema(description = "음악 생성이 완료된 일시 (완료 또는 실패 시만 제공)", example = "2025-08-19T14:35:30", required = false, nullable = true)
    private LocalDateTime completedAt;

    @Schema(description = "클라이언트가 다음 폴링을 수행할 권장 간격 (초 단위)", example = "5", required = false, nullable = true)
    private Integer nextPollInterval;

    @Schema(description = "폴링 만료 예정 시간 (이 시간 이후에도 완료되지 않으면 폴링 중단 권장)", example = "2025-08-19T15:00:00", required = false, nullable = true)
    private LocalDateTime pollExpiresAt;

    public static MusicGenerationStatusResponse processing(Long logoSongId, String taskId, LocalDateTime startedAt, String imageUrl) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.PROCESSING)
                .progress(calculateProgress(startedAt))
                .statusMessage("음악 생성 중입니다...")
                .estimatedCompletionSeconds(calculateEstimatedTime(startedAt))
                .startedAt(startedAt)
                .imageUrl(imageUrl)
                .nextPollInterval(5) // 5초 간격
                .pollExpiresAt(startedAt.plusMinutes(30)) // 30분 후 만료
                .build();
    }

    public static MusicGenerationStatusResponse completed(Long logoSongId, String taskId, String audioUrl, 
                                                        String videoUrl, Double duration, LocalDateTime startedAt, 
                                                        LocalDateTime completedAt, String imageUrl) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.COMPLETED)
                .progress(100)
                .statusMessage("음악 생성이 완료되었습니다!")
                .audioUrl(audioUrl)
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .duration(duration)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .nextPollInterval(null) // 더 이상 폴링 불필요
                .build();
    }

    public static MusicGenerationStatusResponse failed(Long logoSongId, String taskId, String errorMessage, 
                                                      LocalDateTime startedAt, String imageUrl) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .taskId(taskId)
                .status(MusicGenerationStatus.FAILED)
                .progress(0)
                .statusMessage("음악 생성에 실패했습니다.")
                .errorMessage(errorMessage)
                .startedAt(startedAt)
                .imageUrl(imageUrl)
                .completedAt(LocalDateTime.now())
                .nextPollInterval(null) // 더 이상 폴링 불필요
                .build();
    }

    public static MusicGenerationStatusResponse pending(Long logoSongId, String imageUrl) {
        return MusicGenerationStatusResponse.builder()
                .logoSongId(logoSongId)
                .status(MusicGenerationStatus.PENDING)
                .progress(0)
                .statusMessage("음악 생성 준비 중입니다...")
                .imageUrl(imageUrl)
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