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
}
