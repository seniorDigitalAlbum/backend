package com.chimaenono.dearmind.MediaFile;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "미디어 파일 엔티티")
public class MediaFile {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "파일 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "conversation_id", nullable = false)
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Column(name = "message_id")
    @Schema(description = "메시지 ID", example = "1")
    private Long messageId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    @Schema(description = "파일 타입", example = "AUDIO")
    private FileType fileType;
    
    @Column(name = "file_path", nullable = false)
    @Schema(description = "실제 파일 경로", example = "/uploads/audio_123.wav")
    private String filePath;
    
    @Column(name = "file_size")
    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long fileSize;
    
    @Column(name = "duration")
    @Schema(description = "재생 시간 (초)", example = "30.5")
    private Double duration;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "파일 생성 시간")
    private LocalDateTime createdAt;
    
    public enum FileType {
        AUDIO, VIDEO, AUDIO_VIDEO
    }
} 