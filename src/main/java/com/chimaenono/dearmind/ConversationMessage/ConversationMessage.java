package com.chimaenono.dearmind.conversationMessage;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;

@Entity
@Table(name = "conversation_messages")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "대화 메시지 엔티티")
public class ConversationMessage {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "메시지 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "conversation_id", nullable = false)
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    @Schema(description = "발신자 타입", example = "USER")
    private SenderType senderType;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    @Schema(description = "메시지 내용", example = "안녕하세요! 오늘은 어떤 이야기를 나누고 싶으신가요?")
    private String content;
    
    @Column(name = "timestamp", nullable = false)
    @Schema(description = "메시지 전송 시간")
    private LocalDateTime timestamp;
    
    // 사용자 감정 분석 결과와의 1:1 관계 (USER 메시지에만 존재)
    @OneToOne(mappedBy = "conversationMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // 순환 참조 방지
    @Schema(description = "사용자 감정 분석 결과 (USER 메시지에만 존재)")
    private UserEmotionAnalysis emotionAnalysis;
    
    public enum SenderType {
        USER, AI
    }
} 