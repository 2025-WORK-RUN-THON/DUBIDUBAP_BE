package com.guineafigma.domain.logosong.dto.request;

import com.guineafigma.common.enums.MusicGenre;
import com.guineafigma.common.enums.VersionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "로고송 생성 요청")
public class LogoSongCreateRequest {

    @Schema(description = "서비스명", example = "로고송 카페")
    @NotBlank(message = "서비스명은 필수입니다.")
    private String serviceName;

    @Schema(description = "슬로건", example = "당신의 브랜드를 노래로")
    private String slogan;

    @Schema(description = "업종", example = "음식점")
    private String industry;

    @Schema(description = "마케팅 아이템", example = "브랜드 인지도 향상")
    private String marketingItem;

    @Schema(description = "타겟 고객", example = "20-30대 여성")
    private String targetCustomer;

    @Schema(description = "분위기/톤", example = "신뢰감,경쾌함")
    private String moodTone;

    @Schema(description = "음악 장르")
    @NotNull(message = "음악 장르는 필수입니다.")
    private MusicGenre musicGenre;

    @Schema(description = "버전 타입")
    @NotNull(message = "버전 타입은 필수입니다.")
    private VersionType version;

    @Schema(description = "추가 정보")
    private String additionalInfo;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "로고송 URL")
    private String logosongUrl;
}