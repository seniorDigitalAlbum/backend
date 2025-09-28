package com.chimaenono.dearmind.microphone;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "microphone_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이크 세션 엔티티")
public class MicrophoneSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "세션 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "session_id", nullable = false, unique = true)
    @Schema(description = "WebSocket 세션 ID", example = "session_12345")
    private String sessionId;
    
    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Column(name = "status")
    @Schema(description = "마이크 상태", example = "ACTIVE")
    private String status; // ACTIVE, INACTIVE, RECORDING
    
    @Column(name = "audio_format")
    @Schema(description = "오디오 포맷", example = "WAV")
    private String audioFormat; // WAV, MP3, AAC
    
    @Column(name = "sample_rate")
    @Schema(description = "샘플 레이트", example = "44100")
    private Integer sampleRate; // 44100, 48000, 16000
    
    @Column(name = "created_at")
    @Schema(description = "세션 생성 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    @Schema(description = "세션 종료 시간")
    private LocalDateTime endedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = "ACTIVE";
        audioFormat = "WAV";
        sampleRate = 44100;
    }
} 