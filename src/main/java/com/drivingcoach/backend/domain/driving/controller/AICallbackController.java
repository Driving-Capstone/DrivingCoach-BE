package com.drivingcoach.backend.domain.driving.controller;

import com.drivingcoach.backend.domain.driving.domain.constant.DrivingFeedbackType; // Enum 임포트
import com.drivingcoach.backend.domain.driving.domain.dto.response.AIAnalysisResultDto;
import com.drivingcoach.backend.domain.driving.service.DrivingService; // 저장용
import com.drivingcoach.backend.domain.driving.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai-callback")
@RequiredArgsConstructor
public class AICallbackController {

    private final WebSocketSessionService sessionService;
    private final DrivingService drivingService; // (추가!) 이벤트 저장 위해 필요

    @PostMapping("/{recordId}")
    public void handleAIAnalysisResult(
            @PathVariable String recordId, // String으로 들어오지만 Long으로 변환 필요
            @RequestBody AIAnalysisResultDto resultDto
    ) {
        log.info("[AI-Callback] 결과 도착: recordId={}, data={}", recordId, resultDto);

        // 1. (기존) 프론트엔드에 AI 시각화 데이터 전송
        sessionService.sendResultToSession(recordId, resultDto);

        // 2. (신규) 이벤트 감지 및 처리 로직
        if (resultDto.getDetectedEventIds() != null && !resultDto.getDetectedEventIds().isEmpty()) {
            processDetectedEvents(Long.valueOf(recordId), resultDto);
        }
    }

    /**
     * 감지된 이벤트를 DB에 저장하고, 프론트엔드에 음성 피드백 메시지를 전송
     */
    private void processDetectedEvents(Long recordId, AIAnalysisResultDto dto) {
        for (Integer eventId : dto.getDetectedEventIds()) {
            DrivingFeedbackType feedback = DrivingFeedbackType.fromId(eventId);
            if (feedback == null) continue;

            // A. DB 저장 (DrivingEvent)
            try {
                // DrivingService의 addEvent 메서드 활용 (userId는 세션 등에서 가져오거나, recordId로 조회해야 함)
                // 여기서는 단순히 recordId 기준으로 저장한다고 가정 (서비스 메서드 보완 필요할 수 있음)
                // *주의: userId가 필요한데 콜백에는 없음. DrivingService에서 recordId만으로 저장하는 메서드 추가 권장.
                // 임시로 userId=null 또는 record 소유자 조회 로직 사용해야 함.

                drivingService.addSystemEvent(recordId, feedback); // (DrivingService에 이 메서드 추가 필요)
                log.info("[Event] 이벤트 저장 완료: {}", feedback.getEventName());

            } catch (Exception e) {
                log.error("[Event] 저장 실패: {}", e.getMessage());
            }

            // B. 프론트엔드 음성 피드백 전송 (WebSocket)
            // 프론트가 식별할 수 있는 별도 타입의 메시지를 보냅니다.
            Map<String, Object> feedbackMsg = new HashMap<>();
            feedbackMsg.put("type", "FEEDBACK_VOICE");
            feedbackMsg.put("eventId", eventId);
            feedbackMsg.put("message", feedback.getFeedbackMessage()); // "신호 위반입니다" 등

            sessionService.sendResultToSession(String.valueOf(recordId), feedbackMsg);
            log.info("[WS] 피드백 전송: {}", feedback.getFeedbackMessage());
        }
    }
}