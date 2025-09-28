package com.chimaenono.dearmind.notification;

import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final NotificationWebSocketService webSocketService;
    
    /**
     * 알람을 생성합니다.
     */
    @Transactional
    public Notification createNotification(Long userId, Long senderId, Notification.NotificationType type, 
                                         String title, String content, Long relatedId) {
        log.info("알람 생성: userId={}, senderId={}, type={}, title={}", userId, senderId, type, title);
        
        Notification notification = Notification.builder()
                .userId(userId)
                .senderId(senderId)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        
        // WebSocket으로 실시간 알람 전송
        webSocketService.sendNotificationToUser(userId, savedNotification);
        
        // 알람 개수 제한 (최근 100개만 유지)
        limitNotificationCount(userId);
        
        log.info("알람 생성 완료: notificationId={}", savedNotification.getId());
        return savedNotification;
    }
    
    /**
     * 보호자 연결 요청 알람을 생성합니다.
     */
    @Transactional
    public Notification createGuardianRequestNotification(Long guardianId, Long seniorId, Long relationshipId) {
        log.info("보호자 연결 요청 알람 생성: guardianId={}, seniorId={}, relationshipId={}", guardianId, seniorId, relationshipId);
        
        // 시니어 정보 조회
        User senior = userService.findById(seniorId)
                .orElseThrow(() -> new RuntimeException("시니어를 찾을 수 없습니다: " + seniorId));
        
        String title = "시니어 연결 요청";
        String content = String.format("%s님이 연결을 요청했습니다", senior.getNickname());
        
        return createNotification(guardianId, seniorId, Notification.NotificationType.GUARDIAN_REQUEST, 
                                title, content, relationshipId);
    }
    
    /**
     * 보호자 연결 요청 승인 알람을 생성합니다.
     */
    @Transactional
    public Notification createGuardianRequestApprovedNotification(Long guardianId, Long seniorId, Long relationshipId) {
        log.info("보호자 연결 요청 승인 알람 생성: guardianId={}, seniorId={}, relationshipId={}", guardianId, seniorId, relationshipId);
        
        // 시니어 정보 조회
        User senior = userService.findById(seniorId)
                .orElseThrow(() -> new RuntimeException("시니어를 찾을 수 없습니다: " + seniorId));
        
        String title = "연결 요청이 승인되었습니다";
        String content = String.format("%s님이 연결 요청을 승인했습니다", senior.getNickname());
        
        return createNotification(guardianId, seniorId, Notification.NotificationType.GUARDIAN_REQUEST_APPROVED, 
                                title, content, relationshipId);
    }
    
    /**
     * 보호자 연결 요청 거절 알람을 생성합니다.
     */
    @Transactional
    public Notification createGuardianRequestRejectedNotification(Long guardianId, Long seniorId, Long relationshipId) {
        log.info("보호자 연결 요청 거절 알람 생성: guardianId={}, seniorId={}, relationshipId={}", guardianId, seniorId, relationshipId);
        
        // 시니어 정보 조회
        User senior = userService.findById(seniorId)
                .orElseThrow(() -> new RuntimeException("시니어를 찾을 수 없습니다: " + seniorId));
        
        String title = "연결 요청이 거절되었습니다";
        String content = String.format("%s님이 연결 요청을 거절했습니다", senior.getNickname());
        
        return createNotification(guardianId, seniorId, Notification.NotificationType.GUARDIAN_REQUEST_REJECTED, 
                                title, content, relationshipId);
    }
    
    /**
     * 사용자의 알람 목록을 조회합니다.
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        log.info("사용자 알람 목록 조회: userId={}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 사용자의 알람 목록을 페이지네이션으로 조회합니다.
     */
    public Page<Notification> getNotificationsByUserId(Long userId, int page, int size) {
        log.info("사용자 알람 목록 조회 (페이지네이션): userId={}, page={}, size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 사용자의 읽지 않은 알람 개수를 조회합니다.
     */
    public long getUnreadNotificationCount(Long userId) {
        log.info("읽지 않은 알람 개수 조회: userId={}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * 사용자의 읽지 않은 알람 목록을 조회합니다.
     */
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        log.info("읽지 않은 알람 목록 조회: userId={}", userId);
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 특정 알람을 읽음 처리합니다.
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        log.info("알람 읽음 처리: notificationId={}, userId={}", notificationId, userId);
        
        int updatedRows = notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        boolean success = updatedRows > 0;
        
        if (success) {
            log.info("알람 읽음 처리 완료: notificationId={}", notificationId);
        } else {
            log.warn("알람 읽음 처리 실패: notificationId={}, userId={}", notificationId, userId);
        }
        
        return success;
    }
    
    /**
     * 사용자의 모든 알람을 읽음 처리합니다.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("모든 알람 읽음 처리: userId={}", userId);
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("모든 알람 읽음 처리 완료: userId={}", userId);
    }
    
    /**
     * 알람을 삭제합니다.
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        log.info("알람 삭제: notificationId={}, userId={}", notificationId, userId);
        
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent() && notificationOpt.get().getUserId().equals(userId)) {
            notificationRepository.deleteById(notificationId);
            log.info("알람 삭제 완료: notificationId={}", notificationId);
            return true;
        } else {
            log.warn("알람 삭제 실패: notificationId={}, userId={}", notificationId, userId);
            return false;
        }
    }
    
    /**
     * 알람 개수를 제한합니다 (최근 100개만 유지).
     */
    @Transactional
    public void limitNotificationCount(Long userId) {
        log.info("알람 개수 제한: userId={}", userId);
        
        Pageable pageable = PageRequest.of(100, 1); // 100번째부터 1개씩
        List<Long> oldNotificationIds = notificationRepository.findNotificationIdsByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        if (!oldNotificationIds.isEmpty()) {
            notificationRepository.deleteAllById(oldNotificationIds);
            log.info("오래된 알람 삭제 완료: userId={}, deletedCount={}", userId, oldNotificationIds.size());
        }
    }
    
    /**
     * 오래된 알람을 삭제합니다 (30일 이상).
     */
    @Transactional
    public void deleteOldNotifications(Long userId) {
        log.info("오래된 알람 삭제: userId={}", userId);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotificationsByUserId(userId, cutoffDate);
        
        log.info("오래된 알람 삭제 완료: userId={}, cutoffDate={}", userId, cutoffDate);
    }
}
