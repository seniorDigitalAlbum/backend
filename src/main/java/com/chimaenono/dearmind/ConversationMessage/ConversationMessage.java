package com.chimaenono.dearmind.conversationMessage;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

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
    
    @Column(name = "audio_file_path")
    @Schema(description = "음성 파일 경로", example = "/audio/message_1.wav")
    private String audioFilePath;
    
    @Column(name = "video_file_path")
    @Schema(description = "비디오 파일 경로", example = "/video/message_1.mp4")
    private String videoFilePath;
    
    @Column(name = "timestamp", nullable = false)
    @Schema(description = "메시지 전송 시간")
    private LocalDateTime timestamp;
    
    public enum SenderType {
        USER, AI
    }
} 