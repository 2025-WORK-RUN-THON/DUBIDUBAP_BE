package com.guineafigma.domain.logosong.dto.response;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.common.enums.VersionType;
import com.guineafigma.domain.logosong.entity.LogoSong;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "로고송 응답")
public class LogoSongResponse {

    @Schema(description = "로고송 고유 식별자", example = "123", required = true, nullable = false)
    private Long id;

    @Schema(description = "브랜드 대표 이미지 URL - 업로드 후 생성되는 S3 URL (출력 전용)", example = "https://dubidubap-bucket.s3.ap-northeast-2.amazonaws.com/images/cafe-logo-123.jpg")
    private String imageUrl;

    @Schema(description = "생성된 로고송 음원 파일 URL - 시스템에서 생성 후 제공되는 S3 URL (출력 전용)", example = "https://dubidubap-bucket.s3.ap-northeast-2.amazonaws.com/music/logosong-123.mp3")
    private String logosongUrl;

    @Schema(description = "브랜드/서비스명", example = "카페 뒤비뒤밥", required = true, nullable = false)
    private String serviceName;

    @Schema(description = "브랜드 슬로건", example = "맛있는 만남, 따뜻한 사람들")
    private String slogan;

    @Schema(description = "비즈니스 업종", example = "카페 및 베이커리")
    private String industry;

    @Schema(description = "마케팅 포인트", example = "수제 빵, 신선한 재료, 따뜻한 대접")
    private String marketingItem;

    @Schema(description = "주요 타겟 고객층", example = "20-40대 여성, 카페를 즈기는 직장인")
    private String targetCustomer;

    @Schema(description = "음악의 분위기와 톤", example = "따뜻함, 편안함, 의류슴, 포근함")
    private String moodTone;

    @Schema(description = "사용된 음악 장르", example = "Pop", required = true, nullable = false)
    private String musicGenre;

    @Schema(description = "로고송 길이 버전", example = "SHORT", required = true, nullable = false)
    private VersionType version;

    @Schema(description = "기타 추가 요청사항", example = "담밀죽이 대표 메뉴이고, 오래된 전통을 자랑하는 마을 카페")
    private String additionalInfo;

    @Schema(description = "사용자들의 좋아요 개수", example = "42", required = true, nullable = false)
    private Integer likeCount;

    @Schema(description = "로고송 재생 및 조회 횟수", example = "1250", required = true, nullable = false)
    private Integer viewCount;

    @Schema(description = "로고송 생성 일시", example = "2024-01-15T14:30:00", required = true, nullable = false)
    private LocalDateTime createdAt;

    @Schema(description = "마지막 수정 일시", example = "2024-01-15T15:45:00", required = true, nullable = false)
    private LocalDateTime updatedAt;

    @Schema(description = "AI가 생성한 로고송 가사", example = "카페 뒤비뒤밥에서\n따뜻한 커피 한 잔\n달콤한 케이크와 함께\n행복한 시간")
    private String lyrics;

    @Schema(description = "비디오 제작을 위한 가이드라인", example = "카페 내부 전경, 커피 제작 과정, 고객들의 웃는 모습, 따뜻한 조명과 나무 인테리어 강조")
    private String videoGuideline;

    @Schema(description = "음악 생성 진행 상태", example = "COMPLETED")
    private MusicGenerationStatus musicStatus;

    @Schema(description = "Suno AI로 생성된 음악 파일 URL - 자동 생성되는 음원 다운로드 링크 (출력 전용)", example = "https://cdn1.suno.ai/550e8400-e29b-41d4-a716-446655440000.mp3")
    private String generatedMusicUrl;

    @Schema(description = "음악 생성 완료 일시", example = "2024-01-15T15:20:00")
    private LocalDateTime generatedAt;

    @Schema(description = "현재 사용자의 좋아요 여부 (로그인 시에만 제공)", example = "true")
    private Boolean isLiked;

    public static LogoSongResponse from(LogoSong logoSong) {
        return from(logoSong, null);
    }

    public static LogoSongResponse from(LogoSong logoSong, Boolean isLiked) {
        return LogoSongResponse.builder()
                .id(logoSong.getId())
                .imageUrl(logoSong.getImageUrl())
                .logosongUrl(logoSong.getLogosongUrl())
                .serviceName(logoSong.getServiceName())
                .slogan(logoSong.getSlogan())
                .industry(logoSong.getIndustry())
                .marketingItem(logoSong.getMarketingItem())
                .targetCustomer(logoSong.getTargetCustomer())
                .moodTone(logoSong.getMoodTone())
                .musicGenre(logoSong.getMusicGenre())
                .version(logoSong.getVersion())
                .additionalInfo(logoSong.getAdditionalInfo())
                .likeCount(logoSong.getLikeCount())
                .viewCount(logoSong.getViewCount())
                .lyrics(logoSong.getLyrics())
                .videoGuideline(logoSong.getVideoGuideline())
                .musicStatus(logoSong.getMusicStatus())
                .generatedMusicUrl(logoSong.getGeneratedMusicUrl())
                .generatedAt(logoSong.getGeneratedAt())
                .createdAt(logoSong.getCreatedAt())
                .updatedAt(logoSong.getUpdatedAt())
                .isLiked(isLiked)
                .build();
    }
}