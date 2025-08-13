package com.guineafigma.global.config.security.jwt;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.global.config.properties.Constants;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JWT 필터 처리 시작 - URI: {}", requestURI);
        
        try {
            String token = resolveToken(request);
            log.debug("추출된 토큰: {}", token != null ? "토큰 존재" : "토큰 없음");

            if(StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContext에 인증 정보 설정 완료 - 사용자: {}", authentication.getName());
            } else {
                log.debug("토큰이 없거나 유효하지 않음");
            }
            
        } catch (BusinessException e) {
            log.error("JWT 인증 비즈니스 로직 오류: {}", e.getMessage());
            handleJwtException(request, response, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 예상치 못한 오류 발생: {}", e.getMessage());
            handleJwtException(request, response, ErrorCode.INVALID_TOKEN);
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    // 인증 예외 처리 정형화된 구조로 응답하도록 설정 (GlobalExceptionHandler로 처리 불가 해서 직접 처리)
    private void handleJwtException(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) {
        try {
            response.setStatus(errorCode.getStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            
            ApiResponse<Void> errorResponse = ApiResponse.error(errorCode, request.getRequestURI());
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            
        } catch (IOException e) {
            log.error("JWT 예외 응답 작성 중 오류 발생: {}", e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.AUTH_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }
}