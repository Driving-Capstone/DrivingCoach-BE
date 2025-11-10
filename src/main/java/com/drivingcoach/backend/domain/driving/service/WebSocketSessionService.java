package com.drivingcoach.backend.domain.driving.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    // (Key: recordId, Value: 해당 유저의 WebSocket 세션)
    private final Map<String, WebSocketSession> sessionsByRecordId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper; // JSON 변환용

    /**
     * 주행 시작 시, recordId와 세션을 매핑하여 등록
     */
    public void registerSession(String recordId, WebSocketSession session) {
        sessionsByRecordId.put(recordId, session);
        log.info("[WS-Manager] 세션 등록: recordId={}", recordId);
    }

    /**
     * 연결 종료 시, 맵에서 제거
     */
    public void removeSession(String recordId) {
        if (recordId != null) {
            sessionsByRecordId.remove(recordId);
            log.info("[WS-Manager] 세션 제거: recordId={}", recordId);
        }
    }

    /**
     * AI 콜백 컨트롤러가 이 메서드를 호출하여
     * 특정 recordId에 해당하는 유저에게 AI 분석 결과를 전송
     */
    public void sendResultToSession(String recordId, Object aiResultDto) {
        WebSocketSession session = sessionsByRecordId.get(recordId);

        if (session != null && session.isOpen()) {
            try {
                // AI 결과를 프론트가 받을 JSON 형식으로 변환
                // (이 DTO를 프론트와 규격에 맞게 한 번 더 감싸도 좋습니다)
                String jsonPayload = objectMapper.writeValueAsString(aiResultDto);

                session.sendMessage(new TextMessage(jsonPayload));
                log.info("[WS-Manager] AI 결과 전송 성공: recordId={}", recordId);
            } catch (Exception e) {
                log.error("[WS-Manager] AI 결과 전송 실패: recordId={}. Error: {}", recordId, e.getMessage());
            }
        } else {
            log.warn("[WS-Manager] AI 결과를 받을 세션이 없음: recordId={}", recordId);
        }
    }
}