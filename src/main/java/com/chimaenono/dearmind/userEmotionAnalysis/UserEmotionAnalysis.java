package com.chimaenono.dearmind.userEmotionAnalysis;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;

@Entity
@Table(name = "user_emotion_analysis")
@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "사용자 감정 분석 결과 엔티티")
public class UserEmotionAnalysis {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "감정 분석 고유 ID", example = "1")
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "conversation_message_id", nullable = false)
    @JsonIgnore // 순환 참조 방지
    @Schema(description = "대화 메시지 ID", example = "1")
    private ConversationMessage conversationMessage;
    
    @Column(name = "facial_emotion", columnDefinition = "JSON")
    @Schema(description = "표정 감정 분석 결과 (JSON)", example = "{\"final_emotion\":\"joy\",\"confidence\":0.85,\"emotion_count\":{\"joy\":3,\"neutral\":1},\"total_captures\":4}")
    private String facialEmotion;
    
    @Column(name = "speech_emotion", columnDefinition = "JSON")
    @Schema(description = "말 감정 분석 결과 (JSON)", example = "{\"emotion\":\"기쁨\",\"confidence\":0.92}")
    private String speechEmotion;
    
    @Column(name = "combined_emotion")
    @Schema(description = "통합된 최종 감정", example = "joy")
    private String combinedEmotion;
    
    @Column(name = "combined_confidence")
    @Schema(description = "통합된 최종 신뢰도", example = "0.88")
    private Double combinedConfidence;
    
    @Column(name = "combined_distribution", columnDefinition = "JSON")
    @Schema(description = "통합 확률 분포(JSON)", 
            example = "{\"기쁨\":0.62,\"당황\":0.06,\"분노\":0.05,\"불안\":0.09,\"상처\":0.07,\"슬픔\":0.11}")
    private String combinedDistribution;  // p_fused 저장
    
    @Column(name = "analysis_timestamp", nullable = false)
    @Schema(description = "감정 분석 수행 시간")
    private LocalDateTime analysisTimestamp;
    
    @PrePersist
    protected void onCreate() {
        if (analysisTimestamp == null) {
            analysisTimestamp = LocalDateTime.now();
        }
    }
}
