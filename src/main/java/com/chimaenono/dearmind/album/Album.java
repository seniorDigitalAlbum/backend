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
    
    @Column(name = "question_id", nullable = false)
    @Schema(description = "질문 ID", example = "1")
    private Long questionId;
   
    @Column(name = "created_at", nullable = false)
    @Schema(description = "앨범 생성 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "앨범 수정 시간")
    private LocalDateTime updatedAt;
} 