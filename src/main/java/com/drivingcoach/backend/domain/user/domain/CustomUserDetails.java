package com.drivingcoach.backend.domain.user.domain;

import com.drivingcoach.backend.domain.user.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 인증 주체
 * - User 엔티티를 감싸서 SecurityContext에 저장되는 형태
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.loginId = user.getLoginId();
        this.password = user.getPassword();
        this.active = user.isActive();
        // DB에 저장된 role 문자열(예: ROLE_USER, ROLE_ADMIN) → 권한 객체로 변환
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() { // Spring Security에서 username 필드 의미
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 필요 시 만료 로직 반영
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 필요 시 잠금 로직 반영
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 필요 시 자격 증명 만료 로직 반영
    }

    @Override
    public boolean isEnabled() {
        return active; // 비활성화(탈퇴) 사용자는 인증 불가
    }
}
