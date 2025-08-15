package com.chimaenono.dearmind.album;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "albums")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "앨범 엔티티")
public class Album {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "앨범 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID", example = "user123")
    private String userId;
    
    @Column(name = "conversation_id", nullable = false)
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Column(name = "final_emotion")
    @Schema(description = "최종 감정", example = "기쁨")
    private String finalEmotion;
    
    @Column(name = "diary_content", columnDefinition = "TEXT")
    @Schema(description = "일기 내용", example = "오늘은 어린 시절 추억에 대해 이야기하며 따뜻한 기분을 느꼈습니다.")
    private String diaryContent;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "앨범 생성 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "앨범 수정 시간")
    private LocalDateTime updatedAt;
} 