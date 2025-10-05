package com.drivingcoach.backend.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.drivingcoach.backend.global.exception.ErrorCode;
import com.drivingcoach.backend.global.response.ErrorResponse;
import com.drivingcoach.backend.global.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 예외 URI 목록
        List<String> permitAllUris = Arrays.asList(
                "/auth/login", "/auth/signup", "/swagger-ui/", "/swagger-ui.html",
                "/swagger-resources/", "/auth/reissue", "auth/logout"
        );

        // 예외 URI는 필터링 없이 통과
        if (permitAllUris.stream().anyMatch(uri::startsWith) || uri.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증 필수 URI만 검사
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헤더에서 꺼냈기 때문에 accessToken일 것임.
        // String accessToken = request.getHeader("access");
        String accessToken = authHeader.replace("Bearer ", "");
        System.out.println(accessToken);

        // JWT 유효성 검사
        try {
            jwtUtil.isValid(accessToken);
        } catch (ExpiredJwtException e) {
            // error response 설정
            setErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
            return;
            /////////////////// accessToken 만료시 응답형식결정
        }

        /*
        // 블랙리스트 여부 확인
        if (tokenService.isBlacklisted(token)) {
            // error response 설정
            setErrorResponse(response, ErrorCode.TOKEN_ALREADY_LOGOUT);
            return;
        }
         */

        // jwtUtil.getCategory(accessToken); 해서 access인지뭔지 확인가능
        /*
        if (uri.equals("/auth/reissue")) {
            request.setAttribute("refreshToken", token);
        } else {
            request.setAttribute("accessToken", token);
        */


        String category = jwtUtil.getCategory(accessToken);
        if(!category.equals("accessToken")) {
            setErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }
        request.setAttribute("accessToken", accessToken);

        // 토큰 확인되면 일시적인 세션을 만드는 부분. 로그인된 상태로 만들어주는 부분.
        // 사용자 정보 추출
        String email = jwtUtil.getSubject(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // SecurityContext에 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 필터로 넘기기
        filterChain.doFilter(request, response);
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getHttpStatus().value())
                .error(errorCode.getHttpStatus().name())
                .message(errorCode.getMessage())
                .build();

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

