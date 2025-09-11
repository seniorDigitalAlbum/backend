package com.chimaenono.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 감정 분석 저장 요청 DTO")
public class UserEmotionAnalysisRequest {
    
    @NotNull(message = "대화 메시지 ID는 필수입니다")
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
    
    @Schema(description = "표정 감정 분석 데이터")
    private FacialEmotionData facialEmotionData;
    
    @Schema(description = "말 감정 분석 데이터 (JSON 문자열)", example = "{\"emotion\":\"기쁨\",\"confidence\":0.92}")
    private String speechEmotionData;
    
    @Schema(description = "통합된 최종 감정", example = "joy")
    private String combinedEmotion;
    
    @Schema(description = "통합된 최종 신뢰도", example = "0.88")
    private Double combinedConfidence;
}
