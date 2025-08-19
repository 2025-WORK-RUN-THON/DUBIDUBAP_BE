package com.guineafigma.utils;

import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.domain.user.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public class MockAuthHelper {

    /**
     * JWT 토큰을 모킹하는 RequestPostProcessor
     */
    public static RequestPostProcessor mockJwtAuth(Long userId) {
        User mockUser = User.builder()
                .nickname("testUser")
                .password("password")
                .isActive(true)
                .build();
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(mockUser, userId);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userPrincipal, 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(auth);
    }

    /**
     * SecurityContext에 인증 정보 설정
     */
    public static void setUpAuthentication(Long userId) {
        User mockUser = User.builder()
                .nickname("testUser")
                .password("password")
                .isActive(true)
                .build();
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(mockUser, userId);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * SecurityContext 초기화
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 커스텀 어노테이션 - 테스트에서 사용
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithMockJwtUser {
        long userId() default 1L;
        String nickname() default "testUser";
    }

    /**
     * WithMockJwtUser 어노테이션을 위한 SecurityContextFactory
     */
    public static class WithMockJwtUserSecurityContextFactory 
            implements WithSecurityContextFactory<WithMockJwtUser> {
        
        @Override
        public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
            User mockUser = User.builder()
                    .nickname(annotation.nickname())
                    .password("password")
                    .isActive(true)
                    .build();
            // Reflection을 통해 ID 설정
            try {
                java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockUser, annotation.userId());
            } catch (Exception e) {
                // 테스트용으로 ID 설정 실패해도 진행
            }
            
            CustomUserPrincipal userPrincipal = new CustomUserPrincipal(mockUser);
            
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            return context;
        }
    }
}