package com.guineafigma.domain.logosong.dto.response;

import com.guineafigma.common.enums.MusicGenre;
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

    @Schema(description = "로고송 ID")
    private Long id;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "로고송 URL")
    private String logosongUrl;

    @Schema(description = "서비스명")
    private String serviceName;

    @Schema(description = "슬로건")
    private String slogan;

    @Schema(description = "업종")
    private String industry;

    @Schema(description = "마케팅 아이템")
    private String marketingItem;

    @Schema(description = "타겟 고객")
    private String targetCustomer;

    @Schema(description = "분위기/톤")
    private String moodTone;

    @Schema(description = "음악 장르")
    private MusicGenre musicGenre;

    @Schema(description = "버전 타입")
    private VersionType version;

    @Schema(description = "추가 정보")
    private String additionalInfo;

    @Schema(description = "좋아요 개수")
    private Integer likeCount;

    @Schema(description = "조회수")
    private Integer viewCount;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    @Schema(description = "수정일")
    private LocalDateTime updatedAt;

    public static LogoSongResponse from(LogoSong logoSong) {
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
                .createdAt(logoSong.getCreatedAt())
                .updatedAt(logoSong.getUpdatedAt())
                .build();
    }
}