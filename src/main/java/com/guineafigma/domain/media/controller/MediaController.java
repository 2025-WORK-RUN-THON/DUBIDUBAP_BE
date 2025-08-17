package com.guineafigma.domain.media.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.media.dto.response.MediaResponse;
import com.guineafigma.domain.media.dto.response.MultipleMediaUploadResponse;
import com.guineafigma.domain.media.service.MediaService;
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

@Tag(name = "Media", description = "미디어 업로드 및 관리")
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "임시 미디어 업로드", description = "임시 경로에 다중 파일 업로드. 24시간 후 자동 삭제")
    @ApiErrorExamples({
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.IMAGE_SIZE_TOO_LARGE,
            ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED,
            ErrorCode.AUTHENTICATION_REQUIRED
    })
    @PostMapping(value = "/upload/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MultipleMediaUploadResponse> uploadTempMedia(
            @Parameter(description = "업로드할 파일들 (최대 10개)", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        MultipleMediaUploadResponse response = mediaService.uploadTempMedia(files, userPrincipal.getId());
        return ApiResponse.success("임시 업로드 완료. 24시간 내 미사용 시 자동 삭제됩니다.", response);
    }

    @Operation(summary = "미디어 삭제", description = "S3와 DB에서 삭제")
    @ApiErrorExamples({ ErrorCode.INTERNAL_SERVER_ERROR })
    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @Parameter(description = "미디어 ID", example = "1")
            @PathVariable Long mediaId) {
        mediaService.deleteMedia(mediaId);
        return ApiResponse.success("삭제되었습니다.", null);
    }

    @Operation(summary = "미디어 상세 조회", description = "ID로 상세 조회")
    @ApiErrorExamples({ ErrorCode.INTERNAL_SERVER_ERROR })
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResponse> getMedia(
            @Parameter(description = "미디어 ID", example = "1")
            @PathVariable Long mediaId) {
        return ApiResponse.success("조회 성공", mediaService.getMediaById(mediaId));
    }
}


