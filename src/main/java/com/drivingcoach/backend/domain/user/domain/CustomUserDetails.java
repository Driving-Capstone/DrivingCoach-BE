package com.drivingcoach.backend.domain.user.domain;


import com.drivingcoach.backend.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));//.name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLoginId();
    }
    // CustomUserDetails.getUsername()이 email을 반환하도록 구현돼 있어야 합니다.
    // 이래도 되나? getUsername을 쓰도록 강제되는 곳이 있겠지? 오버라이드면.
    // 걍 그쪽을 getEmail로 바꾸는건 안되겠지

    ////////////////////
    public String getRole() {
        return user.getRole();
    }

    public String getLoginId() {return user.getLoginId();}
    public Long getUserId() {return user.getId();}
    public String getName() {return user.getNickname();}

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
        return true; /*user.isActive();*/
    }
}

