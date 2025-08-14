package com.chimaenono.dearmind.conversation;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "대화 세션 엔티티")
public class Conversation {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "대화 세션 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID", example = "user123")
    private String userId;
    
    @Column(name = "question_id", nullable = false)
    @Schema(description = "선택된 질문 ID", example = "1")
    private Long questionId;
    
    @Column(name = "session_id")
    @Schema(description = "카메라/마이크 세션 ID", example = "session_123")
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "대화 상태", example = "ACTIVE")
    private ConversationStatus status;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "대화 시작 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    @Schema(description = "대화 종료 시간")
    private LocalDateTime endedAt;
    
    public enum ConversationStatus {
        ACTIVE, COMPLETED, PAUSED
    }
} 