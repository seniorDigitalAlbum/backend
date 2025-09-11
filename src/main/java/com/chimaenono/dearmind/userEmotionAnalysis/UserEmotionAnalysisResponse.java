package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 감정 분석 응답 DTO")
public class UserEmotionAnalysisResponse {
    
    @Schema(description = "감정 분석 고유 ID", example = "1")
    private Long id;
    
    @Schema(description = "대화 메시지 ID", example = "123")
    private Long conversationMessageId;
    
    @Schema(description = "표정 감정 분석 결과 (JSON)", example = "{\"finalEmotion\":\"joy\",\"totalCaptures\":4}")
    private String facialEmotion;
    
    @Schema(description = "말 감정 분석 결과 (JSON)", example = "{\"emotion\":\"기쁨\",\"confidence\":0.92}")
    private String speechEmotion;
    
    @Schema(description = "통합된 최종 감정", example = "joy")
    private String combinedEmotion;
    
    @Schema(description = "통합된 최종 신뢰도", example = "0.88")
    private Double combinedConfidence;
    
    @Schema(description = "감정 분석 수행 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime analysisTimestamp;
    
    // UserEmotionAnalysis 엔티티로부터 Response DTO 생성
    public static UserEmotionAnalysisResponse from(UserEmotionAnalysis analysis) {
        UserEmotionAnalysisResponse response = new UserEmotionAnalysisResponse();
        response.setId(analysis.getId());
        response.setConversationMessageId(analysis.getConversationMessage().getId());
        response.setFacialEmotion(analysis.getFacialEmotion());
        response.setSpeechEmotion(analysis.getSpeechEmotion());
        response.setCombinedEmotion(analysis.getCombinedEmotion());
        response.setCombinedConfidence(analysis.getCombinedConfidence());
        response.setAnalysisTimestamp(analysis.getAnalysisTimestamp());
        return response;
    }
}
