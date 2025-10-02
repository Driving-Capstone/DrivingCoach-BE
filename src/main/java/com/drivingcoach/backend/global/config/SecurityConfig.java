package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.domain.user.service.CustomUserDetailsService;
import com.drivingcoach.backend.global.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    private static final String[] PUBLIC_APIS = new String[] {
            // Auth
            "/api/auth/**",
            // Health check
            "/actuator/health",
            // Swagger (선택)
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(req -> {
                    CorsConfiguration cfg = new CorsConfiguration();
                    cfg.setAllowedOrigins(List.of("*")); // 필요 시 프론트 도메인으로 제한
                    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
                    cfg.setAllowedHeaders(List.of("*"));
                    cfg.setExposedHeaders(List.of("Authorization", "Location"));
                    cfg.setAllowCredentials(false);
                    return cfg;
                }))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(mgr -> mgr.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_APIS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        // WebSocket handshake (필요 시 공개)
                        .requestMatchers("/driving/**", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .userDetailsService(customUserDetailsService)
                .headers(Customizer.withDefaults());

        return http.build();
    }

    /** PasswordEncoder (BCrypt) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Spring Security AuthenticationManager */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /** (선택) 정적 리소스 보안 제외 */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        "/favicon.ico",
                        "/error",
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
    }

    /** (선택) DaoAuthenticationProvider 커스터마이징이 필요하면 활성화
     @Bean
     public DaoAuthenticationProvider authenticationProvider() {
     DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
     provider.setUserDetailsService(customUserDetailsService);
     provider.setHideUserNotFoundExceptions(false);
     return provider;
     }
     */
}
