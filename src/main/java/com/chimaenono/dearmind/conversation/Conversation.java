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
    
    @Column(name = "camera_session_id")
    @Schema(description = "카메라 세션 ID", example = "camera_session_123")
    private String cameraSessionId;
    
    @Column(name = "microphone_session_id")
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456")
    private String microphoneSessionId;
    
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
    
    @Column(name = "summary", columnDefinition = "TEXT")
    @Schema(description = "대화 내용 요약", example = "사용자가 어린 시절 추억을 이야기하며...")
    private String summary;
    
    @Column(name = "diary", columnDefinition = "TEXT")
    @Schema(description = "생성된 일기 내용", example = "오늘은 정말 특별한 하루였습니다...")
    private String diary;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    @Schema(description = "처리 상태", example = "READY")
    private ProcessingStatus processingStatus;
    
    @Column(name = "flow_pattern")
    @Schema(description = "대화 감정 패턴", example = "U-shape")
    private String flowPattern;                 // "U-shape" 등
    
    @Column(name = "emotion_flow", columnDefinition = "JSON")
    @Schema(description = "대화 감정 흐름 JSON", example = "{\"segments\":[...], \"metrics\":{...}}")
    private String emotionFlow;                 // segments + metrics 전체 JSON
    
    public enum ConversationStatus {
        ACTIVE, COMPLETED, PAUSED
    }
    
    public enum ProcessingStatus {
        READY, PROCESSING, COMPLETED, ERROR
    }
} 