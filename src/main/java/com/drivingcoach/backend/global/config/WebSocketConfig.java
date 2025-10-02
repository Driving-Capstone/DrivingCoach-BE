package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.domain.driving.websocket.DrivingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정
 * - RN 앱 ↔ 백엔드 간 주행 데이터(영상/음성 청크 메타, 진행 상태 등) 전송 채널
 * - 엔드포인트: /driving
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DrivingWebSocketHandler drivingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(drivingWebSocketHandler, "/driving")
                .setAllowedOriginPatterns("*"); // 필요 시 프론트 도메인으로 제한
    }
}
