// src/main/java/com/drivingcoach/backend/domain/driving/service/AIAnalysisService.java
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
    private String aiServerUrl; // application.yml에 설정할 AI 서버 주소

    /**
     * AI 서버에 S3 Key를 전송하여 비동기 분석을 요청합니다.
     */
    @Async // 비동기 실행 (WebSocket 핸들러를 막지 않기 위해)
    public void triggerAIAnalysis(String s3Key) {
        // AI 서버의 새 엔드포인트
        String url = aiServerUrl + "/analyze_s3_video";

        // 1. 요청 DTO 생성
        S3AnalysisRequestDto requestDto = new S3AnalysisRequestDto(s3Key);

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<S3AnalysisRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            log.info("[AI] S3 Key 전송 시작: {} -> {}", s3Key, url);

            // 3. AI 서버로 POST 요청 (결과는 일단 로그로만 확인)
            // TODO: AI 서버의 응답(BatchDetectionOutput)을 받을 DTO를 만들고,
            //       이 결과를 DrivingRecord 등에 저장하는 로직이 필요할 수 있습니다.
            String response = restTemplate.postForObject(url, entity, String.class);

            log.info("[AI] S3 Key 전송 성공: {}, 응답: {}", s3Key, response);

        } catch (Exception e) {
            log.error("[AI] S3 Key 전송 실패: {}, 에러: {}", s3Key, e.getMessage());
            // TODO: 실패 시 재시도 로직 또는 관리자 알림
        }
    }

    /** AI 서버 /analyze_s3_video 엔드포인트에 맞는 요청 DTO */
    @Getter
    @Setter
    @RequiredArgsConstructor
    private static class S3AnalysisRequestDto {
        private final String s3_key;
    }
}