package com.chimaenono.dearmind.conversation;

import com.chimaenono.dearmind.music.MusicRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일기 조회 응답 DTO")
public class DiaryResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "대화 내용 요약", example = "사용자가 어린 시절 추억을 이야기하며...")
    private String summary;
    
    @Schema(description = "생성된 일기 내용", example = "오늘은 정말 특별한 하루였습니다...")
    private String diary;
    
    @Schema(description = "감정 분석 요약")
    private EmotionSummary emotionSummary;
    
    @Schema(description = "추천 음악 목록")
    private List<MusicRecommendation> musicRecommendations;
    
    @Schema(description = "응답 메시지", example = "일기를 성공적으로 조회했습니다.")
    private String message;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "감정 분석 요약 DTO")
    public static class EmotionSummary {
        @Schema(description = "주요 감정", example = "기쁨")
        private String dominantEmotion;
        
        @Schema(description = "감정별 개수", example = "{\"기쁨\": 3, \"슬픔\": 1}")
        private Map<String, Integer> emotionCounts;
        
        @Schema(description = "평균 신뢰도", example = "0.85")
        private Double averageConfidence;
        
        @Schema(description = "분석된 메시지 수", example = "5")
        private int analyzedMessageCount;
    }
    
    public static DiaryResponse success(Long conversationId, String summary, String diary, 
                                      EmotionSummary emotionSummary, List<MusicRecommendation> musicRecommendations, String message) {
        DiaryResponse response = new DiaryResponse();
        response.setConversationId(conversationId);
        response.setSummary(summary);
        response.setDiary(diary);
        response.setEmotionSummary(emotionSummary);
        response.setMusicRecommendations(musicRecommendations);
        response.setMessage(message);
        response.setSuccess(true);
        return response;
    }
    
    public static DiaryResponse error(String message) {
        DiaryResponse response = new DiaryResponse();
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
}
