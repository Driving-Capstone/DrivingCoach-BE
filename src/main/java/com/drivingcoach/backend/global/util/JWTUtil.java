package com.drivingcoach.backend.global.util;

import com.drivingcoach.backend.domain.user.domain.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTUtil {
    @Value("${jwt.secret-key}")
    private String secretKey;
    @Value("${jwt.access-exp-time}")
    private long ACCESS_TOKEN_EXPIRATION;
    @Value("${jwt.refresh-exp-time}")
    private long REFRESH_TOKEN_EXPIRATION;

    private static final String ACCESS_CATEGORY = "accessToken";
    private static final String REFRESH_CATEGORY = "refreshToken";

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * AccessToken 생성
     *
     * @param customUserDetails
     * @return
     */
    public String createAccessToken(CustomUserDetails customUserDetails) {
        return createToken(ACCESS_CATEGORY, customUserDetails.getLoginId(), customUserDetails.getRole(),customUserDetails.getUserId(), ACCESS_TOKEN_EXPIRATION);
    }

    /**
     * RefreshToken 생성
     *
     * @param customUserDetails
     * @return
     */
    public String createRefreshToken(CustomUserDetails customUserDetails) {
        return createToken(REFRESH_CATEGORY, customUserDetails.getLoginId(), customUserDetails.getRole(),customUserDetails.getUserId(), REFRESH_TOKEN_EXPIRATION);
    }

    /**
     * JWT 토큰 생성 메서드
     *
     * @param category          토큰 카테고리
     * @param identifier        식별자
     * @param role              역할
     * @param userId
     * @param expiredMs         만료 시간
     * @return 생성된 JWT 토큰
     */
    private String createToken(String category, String identifier, String role, Long userId, Long expiredMs){
        return Jwts.builder()
                .subject(identifier)
                .claim("category", category)
                .claim("role", "ROLE_"+role)
                .claim("LoginId", identifier)
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(getSignKey())
                .compact();
    }

    /**
     * JWT 토큰 유효성 검사
     *
     * @param token 검증할 JWT 토큰
     * @return 유효 여부
     */
    public boolean isValid(String token) throws ExpiredJwtException {
        try {
            Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token);
            log.debug("JWT 토큰이 유효합니다.");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
            throw e; // 만료된 토큰 예외를 호출한 쪽으로 전달
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("형식이 잘못된 JWT 토큰입니다: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 null입니다: {}", e.getMessage());
        }
        return false;
    }
    // 운영 시에는 로그 레벨 조정

    /**
     * JWT 토큰에서 Subject 추출
     *
     * @param token JWT 토큰
     * @return 추출된 Subject
     */
    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    /**
     * JWT 토큰에서 LoginId 추출
     *
     * @param token JWT 토큰
     * @return 추출된 Subject
     */
    public String getLoginId(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("LoginId", String.class);
    }
    /**
     * JWT 토큰에서 LoginId 추출
     *
     * @param token JWT 토큰
     * @return 추출된 Subject
     */
    public String getUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId(); // get("id", String.class) 말고 이거로 했는데 되겠지
    }

    /**
     * JWT 토큰에서 category 추출
     *
     * @param token JWT 토큰
     * @return 추출된 category
     */
    public String getCategory(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("category", String.class);
    }

    /**
     * JWT 토큰에서 role 추출
     *
     * @param token JWT 토큰
     * @return 추출된 role
     */
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * 리프레시 토큰 만료 시간 반환
     *
     * @return 리프레시 토큰 만료 시간 (밀리초 단위)
     */
    public long getRefreshExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION;
    }

    /**
     * 토큰 만료 남은 시간 반환
     *
     * @return 토큰 만료 남은 시간 반환 (밀리초 단위)
     */
    public long getRemainingTime(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        System.out.println("///////////////////////////////////////");
        System.out.println(expiration.getTime() - System.currentTimeMillis());
        return expiration.getTime() - System.currentTimeMillis();
    }
}


