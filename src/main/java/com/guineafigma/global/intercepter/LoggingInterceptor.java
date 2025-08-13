package com.guineafigma.global.intercepter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 모든 HTTP 요청을 가로채서 로그를 남기는 역할, 컨트롤러 실행 전후에 자동으로 실행
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    // 컨트롤러 메서드가 실행되기 직전
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        
        // 스레드별 전역 저장소 traceId 저장 같은 요청에서 발생한 로그 추척 가능
        MDC.put("traceId", traceId);
        
        request.setAttribute("startTime", System.currentTimeMillis());

        log.info("=== 요청 시작 ===");
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("Query String: {}", request.getQueryString());
        log.info("Client IP: {}", request.getRemoteAddr());
        return true;
    }

    // 컨트롤러 실행 및 모든 처리가 완료된 후
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {

        // 실행시간 계산
        Long startTime = (Long) request.getAttribute("startTime");
        Long executionTime = System.currentTimeMillis() - startTime;


        log.info("Status: {}", response.getStatus());
        log.info("Execution Time: {}ms", executionTime);
        if (ex != null) {
            log.error("Exception: {}", ex.getMessage());
        }
        log.info("=== 요청 종료 ===\n");

        // 메모리 누수 방지 MDC 정리
        MDC.clear();
    }
} 