package com.guineafigma.domain.system.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.enums.Role;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.common.enums.SocialType;
import com.guineafigma.domain.image.enums.ImageType;
import com.guineafigma.domain.image.service.ImageService;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.security.jwt.JwtTokenProvider;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "System", description = "시스템 관리 및 모니터링 API")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {
    private final DataSource dataSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository memberRepository;
    private final ImageService imageService;

    @Operation(summary = "헬스 체크", description = "서비스 및 DB 상태를 확인합니다.")
    @ApiErrorExamples({
            ErrorCode.INTERNAL_SERVER_ERROR,
    })
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        String dbStatus = "UNKNOWN";
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbStatus = "UP";
            } else {
                dbStatus = "DOWN";
            }
        } catch (SQLException e) {
            dbStatus = "DOWN";
        }
        healthInfo.put("status", "UP");
        healthInfo.put("db", dbStatus);
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("service", "guineafigma-server");

        return ApiResponse.success("서비스가 정상적으로 동작 중입니다.", healthInfo);
    }

    @Operation(summary = "에러 테스트", description = "에러 처리를 테스트합니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @GetMapping("/test-error")
    public ApiResponse<Void> testError() {
        throw new RuntimeException("테스트 에러입니다.");
    }

    @Operation(summary = "유효성 검증 테스트", description = "입력값 유효성 검증을 테스트합니다.")
    @ApiErrorExamples({
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.INVALID_INPUT_VALUE
    })
    @GetMapping("/validation-test")
    public ApiResponse<String> validationTest(
            @Parameter(description = "테스트할 값", example = "test")
            @RequestParam String value
    ) {
        if (value.isEmpty()) {
            throw new RuntimeException("빈 값은 허용되지 않습니다.");
        }
        return ApiResponse.success("유효성 검증 통과", value);
    }

    // @Profile("dev") 추후 dev 환경에서만 사용하도록 수정
    @Operation(summary = "테스트 유저 생성", description = "개발 환경용 테스트 유저를 생성합니다. JWT 인증 테스트를 위해 필요합니다.")
    @PostMapping("/test-user")
    public ApiResponse<String> createTestUser(@RequestParam(defaultValue = "test@guineafigma.com") String email) {
        if (memberRepository.findByEmail(email).isEmpty()) {
            User testUser = User.builder()
                    .email(email)
                    .socialType(SocialType.GOOGLE)
                    .role(Role.USER)
                    .build();
            memberRepository.save(testUser);
            return ApiResponse.success("테스트 유저 생성됨: " + email + " (ID: " + testUser.getId() + ")");
        }
        User existingUser = memberRepository.findByEmail(email).get();
        return ApiResponse.success("테스트 유저 이미 존재: " + email + " (ID: " + existingUser.getId() + ")");
    }

    // @Profile("dev") 추후 dev 환경에서만 사용하도록 수정
    @Operation(summary = "테스트 JWT 토큰 발급", description = "개발 환경용 JWT 토큰을 발급합니다.")
    @PostMapping("/test-token")
    public ApiResponse<String> generateTestToken(@RequestParam(defaultValue = "test@guineafigma.com") String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            email, 
            null, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenProvider.generateToken(auth);
        return ApiResponse.success(token);
    }

    @PostMapping("/test/image-conversion")
    public ImageConversionTestResponse testImageConversion(
            @RequestParam("userId") Long userId,
            @RequestParam("boardId") Long boardId,
            @RequestParam("tempUrl") String tempUrl) {
        
        log.info("이미지 경로 변환 테스트 - 사용자: {}, 게시판: {}, temp URL: {}", userId, boardId, tempUrl);
        
        // temp URL이 포함된 테스트 컨텐츠 생성
        String testContent = String.format("게시글 내용입니다.\n\n%s\n\n이미지가 포함된 내용입니다.", tempUrl);
        
        log.info("원본 컨텐츠: {}", testContent);
        
        // 게시글 타입으로 이미지 경로 변환 (temp -> test_post)
        String processedContent = imageService.processContentAndMigrateImages(
                testContent, 
                ImageType.USER_TEST_POST, 
                userId, 
                boardId
        );
        
        log.info("변환된 컨텐츠: {}", processedContent);
        
        boolean isConverted = !testContent.equals(processedContent);
        
        return ImageConversionTestResponse.builder()
                .success(true)
                .message(isConverted ? "이미지 경로가 성공적으로 변환되었습니다." : "변환할 temp 이미지가 없거나 변환에 실패했습니다.")
                .userId(userId)
                .boardId(boardId)
                .originalTempUrl(tempUrl)
                .originalContent(testContent)
                .processedContent(processedContent)
                .isConverted(isConverted)
                .build();
    }

    @Operation(summary = "게시글 수정 시 이미지 정리 테스트", description = "게시글 수정 시 삭제된 이미지들이 정리되는지 테스트합니다.")
    @PostMapping("/test/post-update-cleanup")
    public PostUpdateCleanupTestResponse testPostUpdateCleanup(
            @RequestParam("postId") Long postId,
            @RequestParam("newContent") String newContent) {
        
        log.info("게시글 수정 시 이미지 정리 테스트 시각 게시글 ID: {}", postId);
        log.info("새 컨텐츠: {}", newContent);
        
        try {
            // 수정 전 해당 게시글의 이미지 목록 조회
            var beforeImages = imageService.findByTypeAndPostId(ImageType.USER_TEST_POST, postId);
            log.info("수정 전 이미지 개수: {}", beforeImages.size());
            
            // 게시글 수정 시 삭제된 이미지 정리 실행
            imageService.processDeletedImagesAfterPostUpdate(ImageType.USER_TEST_POST, postId, newContent);
            
            // 수정 후 해당 게시글의 이미지 목록 조회
            var afterImages = imageService.findByTypeAndPostId(ImageType.USER_TEST_POST, postId);
            log.info("수정 후 이미지 개수: {}", afterImages.size());
            
            int deletedCount = beforeImages.size() - afterImages.size();
            
            return PostUpdateCleanupTestResponse.builder()
                    .success(true)
                    .message(String.format("이미지 정리 완료. %d개 이미지가 삭제되었습니다.", deletedCount))
                    .postId(postId)
                    .newContent(newContent)
                    .beforeImageCount(beforeImages.size())
                    .afterImageCount(afterImages.size())
                    .deletedImageCount(deletedCount)
                    .build();
                    
        } catch (Exception e) {
            log.error("게시글 수정 시 이미지 정리 테스트 실패", e);
            return PostUpdateCleanupTestResponse.builder()
                    .success(false)
                    .message("테스트 실패: " + e.getMessage())
                    .postId(postId)
                    .newContent(newContent)
                    .beforeImageCount(0)
                    .afterImageCount(0)
                    .deletedImageCount(0)
                    .build();
        }
    }
    // 테스트용 응답 DTO
    @lombok.Builder
    @lombok.Getter
    public static class ImageConversionTestResponse {
        private boolean success;
        private String message;
        private Long userId;
        private Long boardId;
        private String originalTempUrl;
        private String originalContent;
        private String processedContent;
        private boolean isConverted;
    }

    @lombok.Builder
    @lombok.Getter
    public static class PostUpdateCleanupTestResponse {
        private boolean success;
        private String message;
        private Long postId;
        private String newContent;
        private int beforeImageCount;
        private int afterImageCount;
        private int deletedImageCount;
    }
}