package com.chimaenono.dearmind.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 사용자의 알람 목록을 최신순으로 조회합니다.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자의 알람 목록을 페이지네이션으로 조회합니다.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자의 읽지 않은 알람 개수를 조회합니다.
     */
    long countByUserIdAndIsReadFalse(Long userId);
    
    /**
     * 사용자의 읽지 않은 알람 목록을 조회합니다.
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 타입의 알람을 조회합니다.
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, Notification.NotificationType type);
    
    /**
     * 사용자의 모든 알람을 읽음 처리합니다.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 알람을 읽음 처리합니다.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * 사용자의 오래된 알람을 삭제합니다 (30일 이상).
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.createdAt < :cutoffDate")
    void deleteOldNotificationsByUserId(@Param("userId") Long userId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * 사용자의 알람 개수를 제한합니다 (최근 100개만 유지).
     */
    @Query("SELECT n.id FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Long> findNotificationIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
