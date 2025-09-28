package com.chimaenono.dearmind.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {
    
    private final ObjectMapper objectMapper;
    
    // 사용자 ID와 WebSocket 세션을 매핑하는 Map
    public final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    /**
     * 사용자의 WebSocket 세션을 등록합니다.
     */
    public void registerUserSession(Long userId, WebSocketSession session) {
        log.info("WebSocket 세션 등록: userId={}, sessionId={}", userId, session.getId());
        userSessions.put(userId, session);
    }
    
    /**
     * 사용자의 WebSocket 세션을 제거합니다.
     */
    public void removeUserSession(Long userId) {
        log.info("WebSocket 세션 제거: userId={}", userId);
        userSessions.remove(userId);
    }
    
    /**
     * 특정 사용자에게 알람을 전송합니다.
     */
    public void sendNotificationToUser(Long userId, Notification notification) {
        log.info("알람 전송: userId={}, notificationId={}", userId, notification.getId());
        
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // 알람 데이터를 JSON으로 변환
                Map<String, Object> notificationData = Map.of(
                    "type", "NOTIFICATION",
                    "data", notification
                );
                
                String message = objectMapper.writeValueAsString(notificationData);
                session.sendMessage(new TextMessage(message));
                
                log.info("알람 전송 성공: userId={}, notificationId={}", userId, notification.getId());
            } catch (IOException e) {
                log.error("알람 전송 실패: userId={}, notificationId={}, error={}", userId, notification.getId(), e.getMessage());
                // 연결이 끊어진 경우 세션 제거
                userSessions.remove(userId);
            }
        } else {
            log.warn("사용자 WebSocket 세션이 없거나 연결이 끊어짐: userId={}", userId);
        }
    }
    
    /**
     * 특정 사용자에게 읽지 않은 알람 개수를 전송합니다.
     */
    public void sendUnreadCountToUser(Long userId, long unreadCount) {
        log.info("읽지 않은 알람 개수 전송: userId={}, count={}", userId, unreadCount);
        
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> countData = Map.of(
                    "type", "UNREAD_COUNT",
                    "data", Map.of("count", unreadCount)
                );
                
                String message = objectMapper.writeValueAsString(countData);
                session.sendMessage(new TextMessage(message));
                
                log.info("읽지 않은 알람 개수 전송 성공: userId={}, count={}", userId, unreadCount);
            } catch (IOException e) {
                log.error("읽지 않은 알람 개수 전송 실패: userId={}, error={}", userId, e.getMessage());
                userSessions.remove(userId);
            }
        }
    }
    
    /**
     * 연결된 사용자 수를 반환합니다.
     */
    public int getConnectedUserCount() {
        return userSessions.size();
    }
    
    /**
     * 특정 사용자가 연결되어 있는지 확인합니다.
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
