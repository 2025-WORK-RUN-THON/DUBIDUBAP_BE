package com.guineafigma.domain.system.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "System", description = "시스템 상태 모니터링 및 관리 API - 서비스 헬스체크, 에러 테스트, 개발 전용 도구")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {
    private final DataSource dataSource;


    @Operation(
        summary = "서비스 헬스 체크", 
        description = "로고송 서비스와 데이터베이스 연결 상태를 확인합니다. " +
                    "서비스 전체의 건강 상태와 데이터베이스 연결 상태를 모니터링하는 데 사용됩니다."
    )
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
        healthInfo.put("service", "dubidubap server");

        return ApiResponse.success("서비스가 정상적으로 동작 중입니다.", healthInfo);
    }

    // removed test endpoints
    
}