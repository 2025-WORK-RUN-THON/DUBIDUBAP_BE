package com.guineafigma.domain.user.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.user.dto.requestDTO.UserLevelUpdateRequestDTO;
import com.guineafigma.domain.user.dto.response.UserLicenseImageResponseDTO;
import com.guineafigma.global.config.SwaggerConfig;
import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.global.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.guineafigma.domain.user.service.UserService;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 마이페이지/레벨/자격증 업로드 API")
public class UserController {
    private final UserService memberService;

    @PatchMapping("/level")
    @Operation(summary = "유저 레벨 변경", description = "마이페이지에서 유저의 레벨을 변경합니다.")
    @SwaggerConfig.ApiSuccessResponse(dataType = Void.class)
    @SwaggerConfig.ApiErrorExamples(value = {ErrorCode.INVALID_INPUT_VALUE, ErrorCode.AUTHENTICATION_REQUIRED})
    public ApiResponse<Void> updateLevel(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @Valid @RequestBody UserLevelUpdateRequestDTO requestDTO) {
        memberService.updateLevel(userPrincipal.getId(), requestDTO);
        return ApiResponse.success(null);
    }

    @PostMapping(path = "/license", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "유저 자격증 이미지 업로드", description = "유저의 자격증 이미지를 업로드합니다.")
    @SwaggerConfig.ApiSuccessResponse(dataType = UserLicenseImageResponseDTO.class)
    @SwaggerConfig.ApiErrorExamples(value = {ErrorCode.INVALID_INPUT_VALUE, ErrorCode.AUTHENTICATION_REQUIRED})
    public ApiResponse<UserLicenseImageResponseDTO> licenseUpload(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Parameter(description = "자격증 이미지 파일", required = true)
            @RequestPart("image") @NotNull MultipartFile image) {

        UserLicenseImageResponseDTO response = memberService.uploadLicense(image, userPrincipal.getId());

        return ApiResponse.success(response);
    }

}
