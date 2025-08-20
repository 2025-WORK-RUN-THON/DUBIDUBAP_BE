package com.guineafigma.global.config.security;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.global.config.security.jwt.JwtAuthenticationFilter;
import com.guineafigma.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .securityContext((securityContext) -> securityContext.requireExplicitSave(false))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**").disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // 인증이 필요한 엔드포인트 (중간 ** 제거한 안전한 패턴)
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/logosongs/my/**").authenticated()
                        
                        .requestMatchers("/api/v1/logosongs/*/generate-music").authenticated()
                        .requestMatchers("/api/v1/logosongs/*/like").authenticated()
                        .requestMatchers("/api/v1/logosongs/*/regenerate-lyrics").authenticated()
                        .requestMatchers("/api/v1/logosongs/*/regenerate-video-guide").authenticated()
                        .requestMatchers("/api/v1/logosongs/*/status").authenticated()
                        .requestMatchers("/api/v1/logosongs/*/visibility").authenticated()
                        .requestMatchers("/api/v1/logosongs/lyrics").authenticated()
                        .requestMatchers("/api/v1/logosongs/with-generation").authenticated()
                        
                        // 공개 엔드포인트
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/logosongs").permitAll()
                        .requestMatchers("/api/v1/logosongs/**").permitAll()
                        // 중복 패턴 제거
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");

            String requestPath = request.getRequestURI();
            ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED, requestPath);
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            String requestPath = request.getRequestURI();
            ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED, requestPath);
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}