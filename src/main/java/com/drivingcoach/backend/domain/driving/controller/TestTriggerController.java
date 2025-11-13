package com.drivingcoach.backend.domain.driving.controller;

import com.drivingcoach.backend.domain.driving.service.AIAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestTriggerController {

    private final AIAnalysisService aiAnalysisService;

    /**
     * AI 콜백 테스트를 강제로 촉발시키는 엔드포인트
     *
     * @param recordId 테스트용 가짜 레코드 ID (예: "test-123")
     * @return 즉시 "작업 요청됨" 메시지 반환
     */
    @GetMapping("/trigger-ai/{recordId}")
    public String triggerAIAnalysisTest(@PathVariable String recordId) {

        // 1. (필수!) S3에 미리 업로드해 둔 테스트 파일 경로
        String testS3Key = "driving/test-video.bin";

        // 2. AI 분석 서비스 호출 (웹소켓 핸들러 대신)
        aiAnalysisService.triggerAIAnalysis(testS3Key, recordId, 1);

        // 3. 즉시 응답 반환
        return "OK. AI Job Triggered for recordId=" + recordId + ". Check Backend Logs for [AI-Callback].";
    }
}