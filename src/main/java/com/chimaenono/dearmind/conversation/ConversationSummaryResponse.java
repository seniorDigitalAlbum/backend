package com.chimaenono.dearmind.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 요약 정보 응답 DTO")
public class ConversationSummaryResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    
    @Schema(description = "선택된 질문", example = "오늘 기분이 어떠세요?")
    private String question;
    
    @Schema(description = "대화 시작 시간", example = "2024-01-15T14:20:00")
    private LocalDateTime startTime;
    
    @Schema(description = "대화 종료 시간", example = "2024-01-15T14:30:00")
    private LocalDateTime endTime;
    
    @Schema(description = "대화 지속 시간 (초)", example = "600")
    private Long duration;
    
    @Schema(description = "대화 상태", example = "COMPLETED")
    private String status;
    
    @Schema(description = "총 메시지 수", example = "10")
    private Integer totalMessageCount;
    
    @Schema(description = "사용자 메시지 수", example = "5")
    private Integer userMessageCount;
    
    @Schema(description = "AI 메시지 수", example = "5")
    private Integer aiMessageCount;
    
    @Schema(description = "감정 분석 요약")
    private EmotionSummary emotionSummary;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "감정 분석 요약 정보")
    public static class EmotionSummary {
        // TODO: 기존 감정 필드 - 새로운 감정 흐름 분석으로 대체 예정
        // @Schema(description = "주요 감정", example = "기쁨")
        // private String dominantEmotion;
        
        @Schema(description = "감정별 개수", example = "{\"기쁨\": 3, \"슬픔\": 1, \"중립\": 1}")
        private Map<String, Integer> emotionCounts;
        
        // @Schema(description = "평균 신뢰도", example = "0.85")
        // private Double averageConfidence;
        
        @Schema(description = "감정 분석된 메시지 수", example = "5")
        private Integer analyzedMessageCount;
    }
}
