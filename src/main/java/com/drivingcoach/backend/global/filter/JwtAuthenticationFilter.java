package com.drivingcoach.backend.global.filter;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import com.drivingcoach.backend.domain.user.domain.entity.User;
import com.drivingcoach.backend.domain.user.repository.UserRepository;
import com.drivingcoach.backend.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - Authorization: Bearer <accessToken> 헤더를 읽어 인증 컨텍스트 설정
 * - 퍼블릭 엔드포인트는 SecurityConfig 에서 permitAll 처리되어 이 필터가 토큰을 못찾아도 통과
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 토큰 검사 제외 패턴 (permitAll 이더라도 명시적으로 스킵하면 약간의 성능 이점) */
    private static final String[] EXCLUDE_PATHS = new String[]{
            "/api/auth/**",
            "/actuator/**",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/", "/error", "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : EXCLUDE_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        try {
            String bearer = req.getHeader(HttpHeaders.AUTHORIZATION);

            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7);

                if (jwtUtil.validateAccessToken(token)) {
                    Claims claims = jwtUtil.getAllClaims(token);
                    String loginId = claims.getSubject();

                    userRepository.findByLoginId(loginId)
                            .filter(User::isActive)
                            .ifPresent(user -> {
                                CustomUserDetails principal = new CustomUserDetails(user);
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        );
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            }
        } catch (Exception e) {
            // 토큰 문제로 인증 실패해도, permitAll 경로는 통과해야 하므로 hard fail 하지 않음
            log.warn("JWT filter error: {}", e.getMessage());
        }

        chain.doFilter(req, res);
    }
}
