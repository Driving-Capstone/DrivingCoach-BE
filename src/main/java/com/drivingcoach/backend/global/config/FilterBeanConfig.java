package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.filter.JWTFilter;
import com.drivingcoach.backend.global.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * ✅ FilterBeanConfig
 *
 * 목적
 *  - {@link com.drivingcoach.backend.global.filter.JWTFilter} 를 스프링 빈으로 등록합니다.
 *  - 보안 설정({@link SecurityConfig})에서 addFilterBefore(...) 로 주입해 필터 체인에 연결합니다.
 *
 * 왜 필요한가?
 *  - JwtAuthenticationFilter 클래스는 @Component 로 등록하지 않았습니다.
 *    (외부 의존성(JwtUtil, UserRepository)을 명시적으로 주입하기 위해 구성 클래스로 생성)
 *  - 이렇게 하면 테스트에서 대체 빈(mock) 주입도 쉬워집니다.
 *
 * 주입 관계
 *  - JwtAuthenticationFilter(jwtUtil, userRepository)
 *    - jwtUtil : 토큰 파싱/검증
 *    - userRepository : 토큰 subject(loginId)로 사용자 활성 여부 확인
 */
@Configuration
@RequiredArgsConstructor
public class FilterBeanConfig {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    /** JwtAuthenticationFilter 를 스프링 컨테이너에 등록 */
    @Bean
    public JWTFilter jwtAuthenticationFilter() {
        return new JWTFilter(jwtUtil, userDetailsService);
    }
}
