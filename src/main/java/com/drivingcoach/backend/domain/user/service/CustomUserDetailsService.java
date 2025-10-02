package com.drivingcoach.backend.domain.user.service;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security용 UserDetailsService 구현
 * - username 파라미터는 loginId로 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * loginId로 활성 사용자 조회 후 Security 주체로 변환
     */
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

        return new CustomUserDetails(user);
    }
}
