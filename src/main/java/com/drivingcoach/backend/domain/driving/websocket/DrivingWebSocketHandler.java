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
import com.drivingcoach.backend.domain.driving.service.DrivingService;

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
    private final DrivingService drivingService;

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
            // ... error handling ...
            return;
        }

        try {
            byte[] bytes = new byte[message.getPayload().remaining()];
            message.getPayload().duplicate().get(bytes);

            // (중요) 이제 recordId가 실제 DB ID(Long)이므로, 이를 기반으로 S3 키 생성
            // 예: driving/105/1723001.bin
            String key = String.format("%s/%s/%d.bin", S3_PREFIX, st.recordId, System.currentTimeMillis());
            s3Uploader.uploadBytes(bytes, key, "application/octet-stream");

            st.incrementChunk();
            safeSendText(session, Json.obj(
                    "type", "CHUNK_STORED",
                    "key", key,
                    "size", bytes.length,
                    "chunkIndex", st.chunkCount
            ));

            // AI 분석 요청 (chunkCount 포함)
            aiAnalysisService.triggerAIAnalysis(key, st.recordId, st.chunkCount);

        } catch (Exception e) {
            log.error("[WS] upload failed", e);
        }
    }

    /* ===================== Handlers ===================== */

    // (3) ★ 핵심 수정 부분: onStart ★
    private void onStart(WebSocketSession session, JsonNode payload) {
        SessionState st = sessions.get(session.getId());
        if (st == null) {
            safeSendText(session, Json.obj("type", "ERROR", "message", "Invalid session"));
            return;
        }

        // 로그인하지 않은 사용자 체크
        if (st.auth.userId == null) {
            safeSendText(session, Json.obj("type", "ERROR", "message", "Login required for recording"));
            return;
        }

        // [수정] 가짜 ID 대신 DB에 실제로 저장하고 진짜 ID(Long)를 받음
        Long dbRecordId = drivingService.startDriving(st.auth.userId, null, null);

        // 세션 상태에 진짜 ID 저장 (String으로 변환하여 호환성 유지)
        String strRecordId = String.valueOf(dbRecordId);
        st.recordId = strRecordId;
        st.chunkCount = 0;

        // 세션 매니저에 등록 (AI 콜백 받을 준비)
        sessionService.registerSession(strRecordId, session);

        // [중요] 프론트엔드에게 진짜 DB ID를 응답 (프론트는 종료 시 이 ID를 사용)
        safeSendText(session, Json.obj("type", "STARTED", "recordId", dbRecordId));

        log.info("[WS] START: sid={}, DB_ID={}, user={}", session.getId(), dbRecordId, st.auth.loginId);
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
