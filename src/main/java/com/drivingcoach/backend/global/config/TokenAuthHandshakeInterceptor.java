package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.global.util.JWTUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JWTUtil jwtUtil;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String query = request.getURI().getQuery(); // e.g. token=Bearer%20xxx.yyy.zzz
        String rawToken = extractTokenFromQuery(query);

        if (!StringUtils.hasText(rawToken)) {
            // 익명 허용: attributes에 anonymous 표시
            attributes.put("authLoginId", "anonymous");
            attributes.put("authUserId", null);
            log.info("[WS-AUTH] no token -> anonymous");
            return true;
        }

        // Bearer prefix 제거
        if (rawToken.startsWith("Bearer ")) rawToken = rawToken.substring(7);

        try {
            // 1) 서명 검증(가능하면)
            boolean valid = jwtUtil.isValid(rawToken);
            if (!valid) {
                attributes.put("authLoginId", "anonymous");
                attributes.put("authUserId", null);
                log.warn("[WS-AUTH] invalid token -> anonymous");
                return true; // 인증 실패라도 연결은 허용(차단하려면 false로)
            }

            // 2) payload 에서 loginId / userId 안전 추출(대소문자 키 대응)
            JwtClaims claims = parseClaims(rawToken);
            String loginId = firstNonEmpty(
                    claims.get("loginId"), claims.get("LoginId"),
                    claims.get("sub") // 최후 fallback
            );
            Long userId = parseLongSafe(
                    firstNonEmpty(claims.get("userId"), claims.get("uid"), claims.get("id"))
            );

            if (!StringUtils.hasText(loginId)) loginId = "anonymous"; // 안전장치

            attributes.put("authLoginId", loginId);
            attributes.put("authUserId", userId);
            log.info("[WS-AUTH] ok loginId={}, userId={}", loginId, userId);

            return true;
        } catch (Exception e) {
            attributes.put("authLoginId", "anonymous");
            attributes.put("authUserId", null);
            log.warn("[WS-AUTH] exception: {} -> anonymous", e.getMessage());
            return true;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractTokenFromQuery(String q) {
        if (!StringUtils.hasText(q)) return null;
        for (String kv : q.split("&")) {
            String[] arr = kv.split("=", 2);
            if (arr.length == 2 && arr[0].equals("token")) {
                return URLDecoder.decode(arr[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String firstNonEmpty(String... cands) {
        for (String s : cands) if (StringUtils.hasText(s)) return s;
        return null;
    }
    private static Long parseLongSafe(String s) {
        try { return StringUtils.hasText(s) ? Long.parseLong(s) : null; }
        catch (Exception ignore) { return null; }
    }

    /** 아주 가벼운 payload 파서 (서명검증은 jwtUtil.isValid로 처리) */
    private JwtClaims parseClaims(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length < 2) return new JwtClaims();
        byte[] json = Base64.getUrlDecoder().decode(parts[1]);
        JsonNode n = om.readTree(json);
        JwtClaims c = new JwtClaims();
        n.fieldNames().forEachRemaining(f -> c.put(f, n.get(f).asText(null)));
        return c;
    }

    private static class JwtClaims extends java.util.HashMap<String, String> {}
}
