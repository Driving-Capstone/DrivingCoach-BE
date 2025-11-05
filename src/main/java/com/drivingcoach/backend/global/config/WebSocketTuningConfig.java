package com.drivingcoach.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketTuningConfig {

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 필요 크기로 조절 (예: 바이너리 10MB, 텍스트 1MB)
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        container.setMaxTextMessageBufferSize(1 * 1024 * 1024);
        container.setAsyncSendTimeout(10_000L);      // 선택: async send 타임아웃
        container.setMaxSessionIdleTimeout(600_000L);// 선택: idle 타임아웃
        return container;
    }
}
