package com.guineafigma.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "로그인/가입 응답")
public class LoginResponse {

    @Schema(description = "사용자 고유 식별자", example = "456")
    private Long userId;

    @Schema(description = "로그인한 사용자의 닉네임", example = "로고송유저")
    private String nickname;

    @Schema(description = "API 인증에 사용할 JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Authorization 헤더에 사용할 토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "토큰 만료까지 남은 시간 (초 단위)", example = "1440000000")
    private Long expiresIn;

    @Schema(description = "이번에 신규 가입한 사용자인지 여부", example = "false")
    private Boolean isNewUser;

    @Schema(description = "로그인 결과 메시지", example = "로그인 성공")
    private String message;
}