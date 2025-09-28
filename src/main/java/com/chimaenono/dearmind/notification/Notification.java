package com.chimaenono.dearmind.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알람 엔티티")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "알람 ID", example = "1")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @Schema(description = "받는 사람 ID (보호자)", example = "1")
    private Long userId;
    
    @Column(name = "sender_id", nullable = false)
    @Schema(description = "보낸 사람 ID (시니어)", example = "2")
    private Long senderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Schema(description = "알람 타입", example = "GUARDIAN_REQUEST")
    private NotificationType type;
    
    @Column(name = "title", nullable = false, length = 100)
    @Schema(description = "알람 제목", example = "시니어 연결 요청")
    private String title;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @Schema(description = "알람 내용", example = "김할머니님이 연결을 요청했습니다")
    private String content;
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead = false;
    
    @Column(name = "related_id")
    @Schema(description = "관련 엔티티 ID (GuardianSeniorRelationship ID)", example = "1")
    private Long relatedId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    @Schema(description = "수정 시간")
    private LocalDateTime updatedAt;
    
    public enum NotificationType {
        GUARDIAN_REQUEST("보호자 연결 요청"),
        GUARDIAN_REQUEST_APPROVED("보호자 연결 요청 승인"),
        GUARDIAN_REQUEST_REJECTED("보호자 연결 요청 거절"),
        CONVERSATION_REMINDER("대화 알림"),
        PHOTO_UPLOAD("사진 업로드 알림"),
        COMMENT_ADDED("댓글 추가 알림"),
        SYSTEM_NOTICE("시스템 공지");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
