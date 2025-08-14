package com.chimaenono.dearmind.question;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회상요법 질문 엔티티")
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "질문 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @Schema(description = "질문 내용", example = "어린 시절 가장 기억에 남는 놀이는 무엇인가요?")
    private String content;
} 