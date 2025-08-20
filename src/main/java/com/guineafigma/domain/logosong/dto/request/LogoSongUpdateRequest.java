package com.guineafigma.domain.logosong.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "로고송 부분 업데이트 요청 (공개 여부, 전시 소개글)")
public class LogoSongUpdateRequest {

    @Schema(description = "공개 여부", example = "true", required = false, nullable = true)
    private Boolean isPublic;

    @Schema(description = "전시 소개글", example = "우리 브랜드는 지역과 함께 자라는 따뜻한 동네 카페입니다.", required = false, nullable = true)
    private String introduction;
}


