package com.drivingcoach.backend.domain.driving.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl; // AI 서버 주소 (예: http://ngrok-url.com)

    @Value("${backend.server.url}") // (필수!) 백엔드 자신의 주소를 yml에 추가해야 함
    private String backendServerUrl; // (예: https://my-backend-api.com)

    /**
     * AI 서버에 비동기 분석을 "요청(Fire)"
     */
    @Async
    public void triggerAIAnalysis(String s3Key, String recordId, int chunkIndex) { // recordId 파라미터 추가

        String url = aiServerUrl + "/analyze_s3_video_async"; // 1. AI 엔드포인트 변경

        // 2. (중요!) AI에 보낼 콜백 URL 생성 (recordId 포함)
        String callbackUrl = String.format("%s/api/ai-callback/%s", backendServerUrl, recordId);

        // 2. (수정!) DTO 생성자에 chunkIndex 전달
        AIAnalysisRequestDto requestDto = new AIAnalysisRequestDto(s3Key, callbackUrl, chunkIndex);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AIAnalysisRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            log.info("[AI] 비동기 작업 요청 시작: recordId={}, key={}", recordId, s3Key);

            // 4. AI 서버로 POST 요청 (이 응답은 "접수 완료" 메시지임)
            String response = restTemplate.postForObject(url, entity, String.class);

            log.info("[AI] 작업 접수 완료: recordId={}, 응답: {}", recordId, response);

        } catch (Exception e) {
            log.error("[AI] 작업 접수 실패: recordId={}, 에러: {}", recordId, e.getMessage());
        }
    }

    /** AI 서버 /analyze_s3_video_async 엔드포인트에 맞는 요청 DTO */
    @Getter
    @Setter
    @RequiredArgsConstructor
    private static class AIAnalysisRequestDto {
        private final String s3FileKey;
        private final String callbackUrl; // AI의 Pydantic 모델과 필드명 일치
        // 3. (추가!) AI가 요구하는 chunkIndex 필드
        private final int chunkIndex;

    }
}