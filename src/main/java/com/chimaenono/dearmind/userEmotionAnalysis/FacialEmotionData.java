package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "표정 감정 분석 데이터 DTO")
public class FacialEmotionData {
    
    @NotBlank(message = "최종 감정은 필수입니다")
    @Schema(description = "최종 감정", example = "joy", required = true)
    private String finalEmotion;
    
    @NotNull(message = "총 캡쳐 횟수는 필수입니다")
    @Schema(description = "총 캡쳐 횟수", example = "4", required = true)
    private Integer totalCaptures;
    
    @NotNull(message = "감정별 개수는 필수입니다")
    @Schema(description = "감정별 개수", example = "{\"joy\":3,\"neutral\":1}", required = true)
    private Map<String, Integer> emotionCounts;
    
    @NotNull(message = "평균 신뢰도는 필수입니다")
    @Schema(description = "평균 신뢰도", example = "0.85", required = true)
    private Double averageConfidence;
    
    @Schema(description = "상세 감정 데이터", example = "[{\"emotion\":\"joy\",\"confidence\":0.92,\"timestamp\":\"2024-01-15T10:30:03\"}]")
    private List<EmotionDetail> emotionDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 감정 상세 정보")
    public static class EmotionDetail {
        @Schema(description = "감정", example = "joy")
        private String emotion;
        
        @Schema(description = "신뢰도", example = "0.92")
        private Double confidence;
        
        @Schema(description = "타임스탬프", example = "2024-01-15T10:30:03")
        private String timestamp;
    }
}
