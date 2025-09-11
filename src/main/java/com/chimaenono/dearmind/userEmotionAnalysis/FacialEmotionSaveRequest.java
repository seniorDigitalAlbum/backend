package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "표정 감정 분석 결과 저장 요청 DTO")
public class FacialEmotionSaveRequest {
    
    @NotNull(message = "대화 메시지 ID는 필수입니다")
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
    
    @NotNull(message = "표정 감정 데이터는 필수입니다")
    @Schema(description = "표정 감정 분석 데이터", example = "{\"finalEmotion\":\"joy\",\"totalCaptures\":4,\"emotionCounts\":{\"joy\":3,\"neutral\":1},\"averageConfidence\":0.85,\"emotionDetails\":[...]}", required = true)
    private FacialEmotionData facialEmotionData;
}
