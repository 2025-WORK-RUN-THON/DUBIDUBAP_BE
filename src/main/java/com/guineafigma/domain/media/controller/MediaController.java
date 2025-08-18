package com.guineafigma.domain.media.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.media.dto.response.MediaResponse;
import com.guineafigma.domain.media.dto.response.MultipleMediaUploadResponse;
import com.guineafigma.domain.media.service.MediaService;
import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.SwaggerConfig.ApiSuccessResponse;
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

@Tag(name = "Media", description = "S3 미디어 파일 업로드 및 관리 API - 이미지, 오디오, 비디오 파일 처리")
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(
        summary = "임시 미디어 업로드", 
        description = "S3 임시 경로에 다중 미디어 파일을 업로드합니다. " +
                    "업로드된 파일은 24시간 후 자동으로 삭제됩니다. " +
                    "지원 형식: JPG, PNG, GIF, MP4, MP3, WAV 등 (최대 10MB)"
    )
    @ApiSuccessResponse(message = "임시 업로드가 성공적으로 처리되었습니다.", dataType = MultipleMediaUploadResponse.class)
    @ApiErrorExamples({
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.MEDIA_SIZE_TOO_LARGE,
            ErrorCode.MEDIA_FORMAT_NOT_SUPPORTED,
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

    @Operation(
        summary = "미디어 삭제", 
        description = "S3 스토리지와 데이터베이스에서 미디어 파일을 완전히 삭제합니다. " +
                    "삭제된 파일은 복구할 수 없으니 주의하세요."
    )
    @ApiSuccessResponse(message = "미디어가 성공적으로 삭제되었습니다.")
    @ApiErrorExamples({ ErrorCode.MEDIA_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR })
    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @Parameter(description = "미디어 ID", example = "1")
            @PathVariable Long mediaId) {
        mediaService.deleteMedia(mediaId);
        return ApiResponse.success("삭제되었습니다.", null);
    }

    @Operation(
        summary = "미디어 상세 조회", 
        description = "미디어 ID를 통해 파일의 상세 정보를 조회합니다. " +
                    "파일명, 크기, 타입, 업로드 날짜, S3 URL 등의 정보를 반환합니다."
    )
    @ApiSuccessResponse(message = "미디어 조회가 성공적으로 처리되었습니다.", dataType = MediaResponse.class)
    @ApiErrorExamples({ ErrorCode.MEDIA_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR })
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResponse> getMedia(
            @Parameter(description = "미디어 ID", example = "1")
            @PathVariable Long mediaId) {
        return ApiResponse.success("조회 성공", mediaService.getMediaById(mediaId));
    }
}


