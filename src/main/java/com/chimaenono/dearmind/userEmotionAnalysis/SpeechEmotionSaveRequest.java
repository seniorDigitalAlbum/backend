package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "말 감정 분석 결과 저장 요청 DTO")
public class SpeechEmotionSaveRequest {
    
    @NotNull(message = "대화 메시지 ID는 필수입니다")
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
    
    @NotBlank(message = "감정은 필수입니다")
    @Schema(description = "분석된 감정", example = "기쁨", required = true)
    private String emotion;
    
    @NotNull(message = "신뢰도는 필수입니다")
    @Schema(description = "감정 분석 신뢰도", example = "0.92", required = true)
    private Double confidence;
    
    @Schema(description = "말 감정 분석 원본 데이터 (JSON)", example = "{\"emotion\":\"기쁨\",\"confidence\":0.92}")
    private String speechEmotionData;
}
