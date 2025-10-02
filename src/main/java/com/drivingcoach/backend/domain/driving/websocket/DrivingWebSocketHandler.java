package com.drivingcoach.backend.domain.driving.websocket;

import com.drivingcoach.backend.global.util.S3Uploader;
import com.drivingcoach.backend.global.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주행 데이터 전송을 위한 WebSocket 핸들러
 *
 * - 엔드포인트: /driving
 * - 프로토콜(제안):
 *   1) 텍스트(START): { "type":"START", "recordId": "optional-uuid" }
 *      → 서버가 recordId 없으면 생성하여 STARTED 로 회신
 *   2) 바이너리(CHUNK): zip/mp4 등 청크를 바이너리로 전송
 *      → 서버는 S3에 저장 후 { "type":"CHUNK_STORED", "key":"s3key", "size":12345 } 회신
 *   3) 텍스트(END): { "type":"END" } → { "type":"ENDED", "recordId":"...", "chunks":N } 회신
 *   4) 텍스트(PING): { "type":"PING" } → { "type":"PONG" } 회신
 *
 * - 인증:
 *   - 쿼리 파라미터로 token(=JWT AccessToken) 전달 가능: ws://.../driving?token=Bearer%20xxx
 *   - 없으면 비로그인 세션으로 처리(추후 Security/HandshakeInterceptor로 대체 가능)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrivingWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Uploader s3Uploader;
    private final JwtUtil jwtUtil;

    /** 세션ID → 상태 */
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    private static final String S3_PREFIX = "driving";  // s3 키 prefix

    /* ===================== Connection Lifecycle ===================== */

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        AuthInfo auth = extractAuth(session);
        sessions.put(sessionId, new SessionState(auth, null, 0, Instant.now()));
        log.info("[WS] connected: sid={}, userLoginId={}, uid={}", sessionId, auth.loginId, auth.userId);
        safeSendText(session, Json.obj("type", "CONNECTED", "sessionId", sessionId));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WS] transport error: sid={}, err={}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionState st = sessions.remove(session.getId());
        log.info("[WS] closed: sid={}, recordId={}, chunks={}, status={}", session.getId(),
                st != null ? st.recordId : null, st != null ? st.chunkCount : 0, status);
    }

    /* ===================== Message Handling ===================== */

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = optText(root, "type").orElse("").toUpperCase();

            switch (type) {
                case "PING" -> safeSendText(session, Json.obj("type", "PONG"));
                case "START" -> onStart(session, root);
                case "END" -> onEnd(session);
                default -> safeSendText(session, Json.obj("type", "ERROR", "message", "Unknown type: " + type));
            }
        } catch (Exception e) {
            log.warn("[WS] handleText error: sid={}, err={}", session.getId(), e.getMessage());
            safeSendText(session, Json.obj("type", "ERROR", "message", "Invalid JSON payload"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionState st = sessions.get(session.getId());
        if (st == null || st.recordId == null) {
            safeSendText(session, Json.obj("type", "ERROR", "message", "Session not started. Send START first."));
            return;
        }

        try {
            byte[] bytes = message.getPayload().array();
            String key = String.format("%s/%s/%d.bin", S3_PREFIX, st.recordId, System.currentTimeMillis());
            s3Uploader.uploadBytes(bytes, key, "application/octet-stream");

            st.incrementChunk();
            safeSendText(session, Json.obj(
                    "type", "CHUNK_STORED",
                    "key", key,
                    "size", bytes.length,
                    "chunkIndex", st.chunkCount
            ));
        } catch (Exception e) {
            log.error("[WS] binary upload failed: sid={}, err={}", session.getId(), e.getMessage(), e);
            safeSendText(session, Json.obj("type", "ERROR", "message", "Upload failed"));
        }
    }

    /* ===================== Handlers ===================== */

    private void onStart(WebSocketSession session, JsonNode payload) {
        SessionState st = sessions.get(session.getId());
        if (st == null) {
            safeSendText(session, Json.obj("type", "ERROR", "message", "Invalid session"));
            return;
        }
        String recordId = optText(payload, "recordId").orElse(UUID.randomUUID().toString());
        st.recordId = recordId;
        st.chunkCount = 0;

        safeSendText(session, Json.obj("type", "STARTED", "recordId", recordId));
        log.info("[WS] START: sid={}, recordId={}, user={}", session.getId(), recordId, st.auth.loginId);
    }

    private void onEnd(WebSocketSession session) {
        SessionState st = sessions.get(session.getId());
        if (st == null || st.recordId == null) {
            safeSendText(session, Json.obj("type", "ERROR", "message", "No active record"));
            return;
        }
        safeSendText(session, Json.obj("type", "ENDED", "recordId", st.recordId, "chunks", st.chunkCount));
        log.info("[WS] END: sid={}, recordId={}, chunks={}", session.getId(), st.recordId, st.chunkCount);
        // 필요 시 여기서 DB에 업로드 메타 정보 집계/저장 로직 추가 (DrivingRecord + S3 key list 등)
    }

    /* ===================== Helpers ===================== */

    private Optional<String> optText(JsonNode node, String field) {
        if (node.hasNonNull(field)) return Optional.ofNullable(node.get(field).asText());
        return Optional.empty();
    }

    private void safeSendText(WebSocketSession session, String json) {
        try {
            if (session.isOpen()) session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("[WS] send failed: sid={}, err={}", session.getId(), e.getMessage());
        }
    }

    private AuthInfo extractAuth(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return AuthInfo.anonymous();

            String q = uri.getQuery(); // e.g., token=Bearer%20xxx
            if (!StringUtils.hasText(q)) return AuthInfo.anonymous();

            for (String kv : q.split("&")) {
                String[] arr = kv.split("=", 2);
                if (arr.length == 2 && arr[0].equals("token")) {
                    String token = java.net.URLDecoder.decode(arr[1], java.nio.charset.StandardCharsets.UTF_8);
                    if (token.startsWith("Bearer ")) token = token.substring(7);
                    if (jwtUtil.validateAccessToken(token)) {
                        String loginId = jwtUtil.getLoginIdFromToken(token);
                        Long userId = jwtUtil.getUserIdFromAccessToken(token);
                        return new AuthInfo(userId, loginId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WS] auth extract failed: {}", e.getMessage());
        }
        return AuthInfo.anonymous();
    }

    /* ===================== Inner Types ===================== */

    @Value
    private static class AuthInfo {
        Long userId;
        String loginId;

        static AuthInfo anonymous() {
            return new AuthInfo(null, "anonymous");
        }
    }

    private static class SessionState {
        final AuthInfo auth;
        String recordId;
        int chunkCount;
        final Instant connectedAt;

        SessionState(AuthInfo auth, String recordId, int chunkCount, Instant connectedAt) {
            this.auth = auth;
            this.recordId = recordId;
            this.chunkCount = chunkCount;
            this.connectedAt = connectedAt;
        }

        void incrementChunk() {
            this.chunkCount++;
        }
    }

    /** 간단 JSON 생성 유틸 (빌더 대용) */
    private static final class Json {
        static String obj(Object... kv) {
            if (kv.length % 2 != 0) throw new IllegalArgumentException("Key/Value must be pairs");
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < kv.length; i += 2) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escape(kv[i].toString())).append("\":");
                Object v = kv[i + 1];
                if (v == null) sb.append("null");
                else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
                else sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
            sb.append('}');
            return sb.toString();
        }

        private static String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
