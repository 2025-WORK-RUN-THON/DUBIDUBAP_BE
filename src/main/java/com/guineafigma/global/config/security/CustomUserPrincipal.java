package com.guineafigma.global.config.security;

import com.guineafigma.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserPrincipal implements UserDetails {
    
    private final Long id;
    private final String nickname;
    private final Boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;
    
    public CustomUserPrincipal(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.isActive = user.getIsActive();
        this.authorities = Collections.singleton(
            new SimpleGrantedAuthority("ROLE_USER")
        );
    }
    
    // UserDetails 구현 메서드들
    @Override
    public String getUsername() {
        return nickname;
    }
    
    @Override
    public String getPassword() {
        return ""; // JWT 토큰 기반 인증이므로 비밀번호 미사용
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    /*
     * 현재 시스템에서 계정 만료, 잠금, 인증 정보 만료, 비활성화와 같은 추가적인 계정 상태 관리를 따로 하지 않음.
     * 모든 계정은 기본적으로 활성화되어 있음. 
     * 추후 필요하다면 추가적인 계정 상태 관리를 위한 메서드를 추가
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}