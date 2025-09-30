package com.chimaenono.dearmind.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private final NotificationWebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 연결 생성: sessionId={}", session.getId());
        
        // URL에서 사용자 ID 추출 (예: /ws/notifications/123)
        String path = session.getUri().getPath();
        String[] pathSegments = path.split("/");
        
        if (pathSegments.length >= 4) {
            try {
                Long userId = Long.parseLong(pathSegments[3]);
                webSocketService.registerUserSession(userId, session);
                log.info("사용자 WebSocket 세션 등록 완료: userId={}, sessionId={}", userId, session.getId());
            } catch (NumberFormatException e) {
                log.error("잘못된 사용자 ID 형식: path={}", path);
                session.close(CloseStatus.BAD_DATA);
            }
        } else {
            log.error("잘못된 WebSocket 경로: path={}", path);
            session.close(CloseStatus.BAD_DATA);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("WebSocket 메시지 수신: sessionId={}, message={}", session.getId(), message.getPayload());
        
        try {
            // 클라이언트로부터 받은 메시지 처리
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageData.get("type");
            
            switch (messageType) {
                case "PING":
                    // 클라이언트의 연결 상태 확인
                    sendPongMessage(session);
                    break;
                case "GET_UNREAD_COUNT":
                    // 읽지 않은 알람 개수 요청
                    handleGetUnreadCountRequest(session, messageData);
                    break;
                default:
                    log.warn("알 수 없는 메시지 타입: type={}", messageType);
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 실패: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket 연결 종료: sessionId={}, status={}", session.getId(), status);
        
        // 사용자 세션 제거
        for (Map.Entry<Long, WebSocketSession> entry : webSocketService.userSessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                webSocketService.removeUserSession(entry.getKey());
                log.info("사용자 WebSocket 세션 제거 완료: userId={}", entry.getKey());
                break;
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류: sessionId={}, error={}", session.getId(), exception.getMessage());
    }
    
    /**
     * PONG 메시지를 전송합니다.
     */
    private void sendPongMessage(WebSocketSession session) throws IOException {
        Map<String, Object> pongData = Map.of(
            "type", "PONG",
            "timestamp", System.currentTimeMillis()
        );
        
        String message = objectMapper.writeValueAsString(pongData);
        session.sendMessage(new TextMessage(message));
        
        log.debug("PONG 메시지 전송: sessionId={}", session.getId());
    }
    
    /**
     * 읽지 않은 알람 개수 요청을 처리합니다.
     */
    private void handleGetUnreadCountRequest(WebSocketSession session, Map<String, Object> messageData) {
        // URL에서 사용자 ID 추출
        String path = session.getUri().getPath();
        String[] pathSegments = path.split("/");
        
        if (pathSegments.length >= 4) {
            try {
                Long userId = Long.parseLong(pathSegments[3]);
                // 읽지 않은 알람 개수를 전송 (실제 구현에서는 NotificationService를 통해 조회)
                log.info("읽지 않은 알람 개수 요청: userId={}", userId);
                // TODO: NotificationService를 통해 실제 읽지 않은 개수 조회 후 전송
            } catch (NumberFormatException e) {
                log.error("잘못된 사용자 ID 형식: path={}", path);
            }
        }
    }
    
    /**
     * 쿼리 파라미터에서 JWT 토큰 추출
     */
    private String getTokenFromQuery(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
