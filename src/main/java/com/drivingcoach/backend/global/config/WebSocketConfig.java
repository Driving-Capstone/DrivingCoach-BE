package com.drivingcoach.backend.global.config;

import com.drivingcoach.backend.domain.driving.websocket.DrivingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;


import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 설정
 * - RN 앱 ↔ 백엔드 간 주행 데이터(영상/음성 청크 메타, 진행 상태 등) 전송 채널
 * - 엔드포인트: /driving
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final DrivingWebSocketHandler drivingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(drivingWebSocketHandler, "/driving")
                .addInterceptors(loggingInterceptor())
                .setAllowedOriginPatterns("*"); // 필요 시 프론트 도메인으로 제한
    }
    @Bean
    public HandshakeInterceptor loggingInterceptor() {
        return new HttpSessionHandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                URI uri = request.getURI();
                List<String> origin = request.getHeaders().get("Origin");
                List<String> auth = request.getHeaders().get("Authorization");
                log.info("[WS-HANDSHAKE] URI={}, Origin={}, Authorization={}, Query={}",
                        uri, origin, auth, uri.getQuery());
                return true; // 여기서 false를 리턴하면 즉시 거절됨
            }
            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception ex) {
                if (ex != null) {
                    log.warn("[WS-HANDSHAKE] afterHandshake error: {}", ex.getMessage());
                }
            }
        };
    }
}


