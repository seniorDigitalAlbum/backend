package com.chimaenono.dearmind.camera;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "camera_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카메라 세션 엔티티")
public class CameraSession {
    
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
    @Schema(description = "카메라 상태", example = "ACTIVE")
    private String status; // ACTIVE, INACTIVE, RECORDING
    
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
    }
} 