package com.drivingcoach.backend.global.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰 유틸
 * - jjwt (io.jsonwebtoken:jjwt-api / -impl / -jackson) 의존성 필요
 * - application.yml 에서 시크릿/만료시간/issuer 를 주입받아 사용
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")           // Base64 인코딩된 시크릿을 권장
    private String secret;

    @Value("${jwt.access-exp-ms:3600000}")   // 기본 1시간
    private long accessExpMs;

    @Value("${jwt.refresh-exp-ms:1209600000}") // 기본 14일
    private long refreshExpMs;

    @Value("${jwt.issuer:driving-coach}")
    private String issuer;

    private Key key;

    @PostConstruct
    void initKey() {
        // secret 이 Base64 라고 가정 (권장). 만약 평문이라면 .getBytes() 사용.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /* ==================== Generate ==================== */

    /** 액세스 토큰 생성: userId, loginId, role 포함 */
    public String generateAccessToken(Long userId, String loginId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("lid", loginId);
        claims.put("role", role);
        return buildToken(claims, loginId, accessExpMs);
    }

    /** 리프레시 토큰 생성: 최소한의 정보만 포함 (loginId) */
    public String generateRefreshToken(String loginId) {
        return buildToken(new HashMap<>(), loginId, refreshExpMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expMs)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* ==================== Validate ==================== */

    public boolean validateAccessToken(String token) {
        return validate(token);
    }

    public boolean validateRefreshToken(String token) {
        return validate(token);
    }

    private boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    /* ==================== Extract ==================== */

    public Claims getAllClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** subject 로 저장한 loginId */
    public String getLoginIdFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public Long getUserIdFromAccessToken(String token) {
        Object v = getAllClaims(token).get("uid");
        return (v instanceof Integer) ? ((Integer) v).longValue() : (Long) v;
    }

    public String getRoleFromAccessToken(String token) {
        Object v = getAllClaims(token).get("role");
        return v != null ? v.toString() : null;
    }

    public Date getExpiration(String token) {
        return getAllClaims(token).getExpiration();
    }
}
