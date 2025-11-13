package com.drivingcoach.backend.domain.driving.websocket;

import com.drivingcoach.backend.domain.driving.service.WebSocketSessionService;
import com.drivingcoach.backend.global.util.S3Uploader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import com.drivingcoach.backend.domain.driving.service.AIAnalysisService; // 1. AI 서비스 임포트

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주행 데이터 전송을 위한 WebSocket 핸들러
 * - 엔드포인트: /ws/driving
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrivingWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Uploader s3Uploader;
    private final AIAnalysisService aiAnalysisService; // 2. AI 서비스 주입
    private final WebSocketSessionService sessionService; // 2. 주입

    /** 세션ID → 상태 */
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    private static final String S3_PREFIX = "driving";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        AuthInfo auth = resolveAuthFromAttributes(session);
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

        // 3. (추가) 세션 맵에서 제거
        sessionService.removeSession(st != null ? st.recordId : null);

        log.info("[WS] closed: sid={}, recordId={}, chunks={}, status={}",
                session.getId(), st != null ? st.recordId : null, st != null ? st.chunkCount : 0, status);
    }

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
            byte[] bytes = new byte[message.getPayload().remaining()];
            message.getPayload().duplicate().get(bytes);

            String key = String.format("%s/%s/%d.bin", S3_PREFIX, st.recordId, System.currentTimeMillis());
            s3Uploader.uploadBytes(bytes, key, "application/octet-stream");

            st.incrementChunk();
            safeSendText(session, Json.obj(
                    "type", "CHUNK_STORED",
                    "key", key,
                    "size", bytes.length,
                    "chunkIndex", st.chunkCount
            ));

            // 4. (수정!) AI 분석 요청 시 recordId도 함께 전달
            aiAnalysisService.triggerAIAnalysis(key, st.recordId, st.chunkCount); // <-- st.chunkCount 추가

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

        // 5. (추가!) 세션 맵에 등록
        sessionService.registerSession(recordId, session);

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
    }

    /* ===================== Helpers ===================== */

    private Optional<String> optText(JsonNode node, String field) {
        if (node.hasNonNull(field)) return Optional.ofNullable(node.get(field).asText());
        return Optional.empty();
    }

    private void safeSendText(WebSocketSession session, String json) {
        try { if (session.isOpen()) session.sendMessage(new TextMessage(json)); }
        catch (IOException e) { log.warn("[WS] send failed: sid={}, err={}", session.getId(), e.getMessage()); }
    }

    private AuthInfo resolveAuthFromAttributes(WebSocketSession session) {
        Object loginId = session.getAttributes().get("authLoginId");
        Object userId  = session.getAttributes().get("authUserId");
        String lid = loginId instanceof String ? (String) loginId : "anonymous";
        Long uid = (userId instanceof Long) ? (Long) userId : null;
        return new AuthInfo(uid, lid);
    }

    /* ===================== Inner Types ===================== */

    @Value
    private static class AuthInfo {
        Long userId;
        String loginId;
        static AuthInfo anonymous() { return new AuthInfo(null, "anonymous"); }
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
        void incrementChunk() { this.chunkCount++; }
    }

    /** 간단 JSON 생성 유틸 */
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
