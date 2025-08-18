package com.guineafigma.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "로그인/가입 응답")
public class LoginResponse {

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "닉네임")
    private String nickname;

    @Schema(description = "JWT 액세스 토큰")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "토큰 만료 시간 (초)")
    private Long expiresIn;

    @Schema(description = "신규 가입 여부")
    private Boolean isNewUser;

    @Schema(description = "메시지")
    private String message;
}