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
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean; // 1. import 추가
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final DrivingWebSocketHandler drivingWebSocketHandler;
    private final TokenAuthHandshakeInterceptor tokenAuthHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(drivingWebSocketHandler, "/ws/driving")
                .addInterceptors(loggingInterceptor(), tokenAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); // 필요 시 프론트 도메인으로 제한
    }

    @Bean
    public HandshakeInterceptor loggingInterceptor() {
        // ... (기존 코드 생략) ...
        return new HttpSessionHandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                URI uri = request.getURI();
                List<String> origin = request.getHeaders().get("Origin");
                List<String> auth = request.getHeaders().get("Authorization");
                log.info("[WS-HANDSHAKE] URI={}, Origin={}, Authorization={}, Query={}",
                        uri, origin, auth, uri.getQuery());
                return true;
            }
        };
    }

    /**
     * 2. 웹소켓 버퍼 크기 설정을 위한 빈 추가
     * 4-5MB를 원하셨으므로 5MB (5 * 1024 * 1024 바이트)로 설정합니다.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        final int FIVE_MB = 5 * 1024 * 1024;

        // 텍스트 메시지 버퍼 크기 5MB로 설정
        container.setMaxTextMessageBufferSize(FIVE_MB);

        // 바이너리 메시지 버퍼 크기 5MB로 설정
        container.setMaxBinaryMessageBufferSize(FIVE_MB);

        return container;
    }
}