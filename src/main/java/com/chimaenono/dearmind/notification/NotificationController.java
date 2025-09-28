package com.chimaenono.dearmind.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알람 관리 API")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 사용자의 알람 목록을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "알람 목록 조회", description = "사용자의 알람 목록을 최신순으로 조회합니다.")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("알람 목록 조회 요청: userId={}", userId);
        
        try {
            List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
            log.info("알람 목록 조회 성공: userId={}, count={}", userId, notifications.size());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("알람 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 사용자의 알람 목록을 페이지네이션으로 조회합니다.
     */
    @GetMapping("/page")
    @Operation(summary = "알람 목록 조회 (페이지네이션)", description = "사용자의 알람 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<Page<Notification>> getNotificationsWithPagination(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("알람 목록 조회 요청 (페이지네이션): userId={}, page={}, size={}", userId, page, size);
        
        try {
            Page<Notification> notifications = notificationService.getNotificationsByUserId(userId, page, size);
            log.info("알람 목록 조회 성공 (페이지네이션): userId={}, totalElements={}", userId, notifications.getTotalElements());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("알람 목록 조회 실패 (페이지네이션): userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 읽지 않은 알람 개수를 조회합니다.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알람 개수 조회", description = "사용자의 읽지 않은 알람 개수를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("읽지 않은 알람 개수 조회 요청: userId={}", userId);
        
        try {
            long unreadCount = notificationService.getUnreadNotificationCount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            
            log.info("읽지 않은 알람 개수 조회 성공: userId={}, count={}", userId, unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("읽지 않은 알람 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 읽지 않은 알람 목록을 조회합니다.
     */
    @GetMapping("/unread")
    @Operation(summary = "읽지 않은 알람 목록 조회", description = "사용자의 읽지 않은 알람 목록을 조회합니다.")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("읽지 않은 알람 목록 조회 요청: userId={}", userId);
        
        try {
            List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userId);
            log.info("읽지 않은 알람 목록 조회 성공: userId={}, count={}", userId, notifications.size());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("읽지 않은 알람 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 알람을 읽음 처리합니다.
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "알람 읽음 처리", description = "특정 알람을 읽음 처리합니다.")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "알람 ID", required = true)
            @PathVariable Long notificationId,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("알람 읽음 처리 요청: notificationId={}, userId={}", notificationId, userId);
        
        try {
            boolean success = notificationService.markAsRead(notificationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            
            if (success) {
                log.info("알람 읽음 처리 성공: notificationId={}", notificationId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("알람 읽음 처리 실패: notificationId={}, userId={}", notificationId, userId);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("알람 읽음 처리 실패: notificationId={}, userId={}, error={}", notificationId, userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 모든 알람을 읽음 처리합니다.
     */
    @PutMapping("/read-all")
    @Operation(summary = "모든 알람 읽음 처리", description = "사용자의 모든 알람을 읽음 처리합니다.")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("모든 알람 읽음 처리 요청: userId={}", userId);
        
        try {
            notificationService.markAllAsRead(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 알람이 읽음 처리되었습니다.");
            
            log.info("모든 알람 읽음 처리 성공: userId={}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("모든 알람 읽음 처리 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 알람을 삭제합니다.
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알람 삭제", description = "특정 알람을 삭제합니다.")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "알람 ID", required = true)
            @PathVariable Long notificationId,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        log.info("알람 삭제 요청: notificationId={}, userId={}", notificationId, userId);
        
        try {
            boolean success = notificationService.deleteNotification(notificationId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            
            if (success) {
                log.info("알람 삭제 성공: notificationId={}", notificationId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("알람 삭제 실패: notificationId={}, userId={}", notificationId, userId);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("알람 삭제 실패: notificationId={}, userId={}, error={}", notificationId, userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
