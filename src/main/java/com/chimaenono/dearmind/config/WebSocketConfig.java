package com.chimaenono.dearmind.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.chimaenono.dearmind.notification.NotificationWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 알람 WebSocket 핸들러 등록
        // 경로: /ws/notifications/{userId}
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications/{userId}")
                .setAllowedOriginPatterns("*") // 모든 Origin 허용 (패턴 사용)
                .withSockJS(); // SockJS 지원 (폴백 옵션)
    }
}
