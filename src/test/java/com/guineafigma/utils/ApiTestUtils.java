package com.guineafigma.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ApiTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 객체를 JSON 문자열로 변환
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    // 성공 응답 검증 매처 (실제 ApiResponse 스키마 반영)
    public static ResultMatcher[] successResponse() {
        return new ResultMatcher[]{
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.status").value(200),
                jsonPath("$.code").value("SUCCESS"),
                jsonPath("$.message").exists()
        };
    }

    // 에러 응답 검증 매처
    public static ResultMatcher[] errorResponse(String errorCode) {
        return new ResultMatcher[]{
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.code").value(errorCode),
                jsonPath("$.message").exists(),
                jsonPath("$.status").exists()
        };
    }

    // 페이지네이션 응답 검증 매처
    public static ResultMatcher[] pagedResponse() {
        return new ResultMatcher[]{
                status().isOk(),
                jsonPath("$.status").value(200),
                jsonPath("$.code").value("SUCCESS"),
                jsonPath("$.data.content").isArray(),
                jsonPath("$.data.pagination.limit").exists(),
                jsonPath("$.data.pagination.currentPage").exists(),
                jsonPath("$.data.pagination.totalPage").exists()
        };
    }

    // 인증 필요 응답 검증
    public static ResultMatcher[] authenticationRequiredResponse() {
        return errorResponse("AUTHENTICATION_REQUIRED");
    }

    // 검증 실패 응답 검증
    public static ResultMatcher[] validationErrorResponse() {
        return errorResponse("VALIDATION_ERROR");
    }

    // 생성 성공 응답 검증
    public static ResultMatcher[] createdResponse() {
        return new ResultMatcher[]{
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.status").value(201),
                jsonPath("$.code").value("SUCCESS"),
                jsonPath("$.message").exists()
        };
    }

    // 빈 성공 응답 검증 (데이터 없음)
    public static ResultMatcher[] emptySuccessResponse() {
        return new ResultMatcher[]{
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.timestamp").exists(),
                jsonPath("$.status").value(200),
                jsonPath("$.code").value("SUCCESS"),
                jsonPath("$.message").exists()
        };
    }

    // 리소스 찾을 수 없음 응답 검증
    public static ResultMatcher[] notFoundResponse(String errorCode) {
        return new ResultMatcher[]{
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON),
                jsonPath("$.status").value(404),
                jsonPath("$.code").value(errorCode)
        };
    }

    // JWT 토큰 헤더
    public static String authHeader(String token) {
        return "Bearer " + token;
    }

    // 기본 테스트 토큰
    public static String defaultTestToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }
}