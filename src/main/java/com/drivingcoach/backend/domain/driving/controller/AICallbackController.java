package com.drivingcoach.backend.domain.driving.controller;

import com.drivingcoach.backend.domain.driving.domain.dto.response.AIAnalysisResultDto;
import com.drivingcoach.backend.domain.driving.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai-callback")
@RequiredArgsConstructor
public class AICallbackController {

    private final WebSocketSessionService sessionService;

    /**
     * AI 서버가 분석 결과를 이 엔드포인트로 POST합니다.
     * (AI의 AsyncCallbackData Pydantic 모델과 형식이 같아야 함)
     */
    @PostMapping("/{recordId}")
    public void handleAIAnalysisResult(
            @PathVariable String recordId,
            @RequestBody AIAnalysisResultDto resultDto
    ) {
        // AI가 s3_file_key와 recordId를 둘 다 보내주므로 로그 강화
        log.info("[AI-Callback] 결과 도착: recordId={}, s3_key={}", recordId, resultDto.getS3FileKey());

        // WebSocket 세션 관리자에게 AI 결과를 전달
        sessionService.sendResultToSession(recordId, resultDto);

        // (AI 서버에게는 빨리 200 OK를 응답하고 종료)
    }
}