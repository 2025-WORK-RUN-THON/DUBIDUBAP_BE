package com.guineafigma.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이미지 업로드 요청")
public class ImageUploadRequest {
    
    @NotNull(message = "이미지 파일은 필수입니다")
    @Schema(description = "업로드할 이미지 파일")
    private MultipartFile file;
    
    @NotNull(message = "업로드 경로는 필수입니다")
    @Schema(description = "S3 업로드 경로", example = "users/1/chat/10/")
    private String uploadPath;
}