package com.guineafigma.domain.image.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.image.dto.response.ImageResponse;
import com.guineafigma.domain.image.dto.response.MultipleImageUploadResponse;
import com.guineafigma.domain.image.service.ImageService;
import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Image", description = "이미지 업로드 및 관리")
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "임시 이미지 업로드", description = "임시 경로에 다중 이미지를 업로드합니다. 24시간 후 자동 삭제됩니다.")
    @ApiErrorExamples({
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.IMAGE_SIZE_TOO_LARGE,
            ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping(value = "/upload/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MultipleImageUploadResponse> uploadTempImages(
            @Parameter(description = "업로드할 이미지 파일들 (최대 10개)", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        
        MultipleImageUploadResponse response = imageService.uploadTempImages(files, userPrincipal.getId());
        return ApiResponse.success("임시 이미지 업로드가 완료되었습니다. 24시간 내에 사용하지 않으면 자동 삭제됩니다.", response);
    }

    

    @Operation(summary = "이미지 삭제", description = "S3와 DB에서 이미지를 삭제합니다.")
    @ApiErrorExamples({
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(
            @Parameter(description = "이미지 ID", example = "1")
            @PathVariable Long imageId
    ) {
        imageService.deleteImage(imageId);
        return ApiResponse.success("이미지가 삭제되었습니다.", null);
    }

    @Operation(summary = "이미지 상세 조회", description = "이미지 ID로 상세 정보를 조회합니다.")
    @ApiErrorExamples({
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @GetMapping("/{imageId}")
    public ApiResponse<ImageResponse> getImage(
            @Parameter(description = "이미지 ID", example = "1")
            @PathVariable Long imageId
    ) {
        ImageResponse image = imageService.getImageById(imageId);
        return ApiResponse.success("이미지 정보를 조회했습니다.", image);
    }
} 