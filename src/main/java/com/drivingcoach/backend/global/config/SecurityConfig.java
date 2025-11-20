package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.global.util.JWTUtil;
import com.drivingcoach.backend.domain.user.service.CustomUserDetailsService;
import com.drivingcoach.backend.global.filter.JWTFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // cors 처리
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // csrf disable
                .csrf(csrf -> csrf.disable()) // 또는 최소한 /driving에 대해서는 CSRF 무시
                //.csrf(csrf -> csrf.ignoringRequestMatchers("/driving/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()  // ✅ WebSocket 핸드셰이크 허용

                )
                .cors(c -> {}) // CORS 기본 허용(원하면 config 추가)
                .headers(h -> h.frameOptions(f -> f.disable()))
                ////////////////////////////////////////////////////////////////////////////

                // form 로그인 방식 disable
                .formLogin(auth -> auth.disable())

                // http basic 인증 방식 disable
                .httpBasic(auth -> auth.disable())


                // 인가 처리
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/**",
                                "api/auth/login", "api/auth/register", "api/auth/reissue",
                                "api/auth/logout", "/api/auth/delete",
                                "/email/send", "/email/verify",
                                "/api/scholarships",
                                "/api/scholarships/**",
                                "/webjars/**",
                                "/ws/**",
                                "/api/ai-callback/**",
                                "/api/test/**",
                                "/api/regions/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/auth/logout").permitAll()       // 프리플라이트 허용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()

                )

                // filter 추가
                /////////////////////////////////////////////// 여기달랐음 jwtutil 다음에 tokenservice 넣음
                .addFilterBefore(new JWTFilter(jwtUtil, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)

                // 세션 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 익명 사용자 허용
                .anonymous(anonymous -> anonymous
                        .principal("anonymousUser")
                        .authorities("ROLE_ANONYMOUS"))

                // 예외 처리: 모든 인증 불필요 경로에서 403 차단 제거
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_OK); // 403 대신 200 OK
                        }));
        return http.build();
    }

    /**
     * CORS 설정 소스 빈
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 모든 Origin 허용
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        // 허용 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 쿠키 허용
        configuration.setAllowCredentials(true);
        // 요청 헤더 허용
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        // 응답 헤더에 'Authorization' 을 노출. 프론트측이 볼 수 있게
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
        // preflight 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

