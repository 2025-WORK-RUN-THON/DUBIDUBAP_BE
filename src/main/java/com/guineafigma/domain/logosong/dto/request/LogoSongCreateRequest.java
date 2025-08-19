package com.guineafigma.domain.logosong.dto.request;

import com.guineafigma.common.enums.VersionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "로고송 생성 요청")
public class LogoSongCreateRequest {

    @Schema(description = "서비스명 - 로고송을 만들 브랜드나 서비스의 이름", example = "카페 뒤비뒤밥")
    @NotBlank(message = "서비스명은 필수입니다.")
    private String serviceName;

    @Schema(description = "브랜드 슬로건 - 브랜드의 핀트가 되는 짧은 문구", example = "맛있는 만남, 따뜻한 사람들")
    private String slogan;

    @Schema(description = "비즈니스 업종 - 음악 스타일과 분위기 결정에 중요한 요소", example = "카페 및 베이커리")
    private String industry;

    @Schema(description = "마케팅 포인트 - 강조하고 싶은 브랜드의 핵심 가치나 제품 특징", example = "수제 빵, 신선한 재료, 따뜻한 대접")
    private String marketingItem;

    @Schema(description = "주요 타겟 고객층 - 연령대, 성별, 라이프스타일 등", example = "20-40대 여성, 카페를 즐기는 직장인")
    private String targetCustomer;

    @Schema(description = "원하는 음악의 분위기와 톤 - 콤마로 구분하여 여러 개 입력 가능", example = "따뜻함, 편안함, 의류슴, 포근함")
    private String moodTone;

    @Schema(description = "로고송에 사용할 음악 장르 - 브랜드 이미지와 어울리는 스타일 선택", example = "Pop")
    @NotBlank(message = "음악 장르는 필수입니다.")
    private String musicGenre;

    @Schema(description = "로고송 길이 - SHORT(15-30초), LONG(45-60초)", example = "SHORT")
    @NotNull(message = "버전 타입은 필수입니다.")
    private VersionType version;

    @Schema(description = "기타 추가 요청사항 - 특별한 단어나 브랜드 스토리 등", example = "담밀죽이 대표 메뉴이고, 오래된 전통을 자랑하는 마을 카페")
    private String additionalInfo;
}