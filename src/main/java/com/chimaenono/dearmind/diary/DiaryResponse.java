package com.chimaenono.dearmind.diary;

import com.chimaenono.dearmind.music.MusicRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일기 조회 응답 DTO")
public class DiaryResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "생성된 일기 내용", example = "오늘은 정말 특별한 하루였습니다...")
    private String diary;
    
    @Schema(description = "추천 음악 목록")
    private List<MusicRecommendation> musicRecommendations;
    
    @Schema(description = "응답 메시지", example = "일기를 성공적으로 조회했습니다.")
    private String message;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    
    public static DiaryResponse success(Long conversationId, String diary, 
                                      List<MusicRecommendation> musicRecommendations, String message) {
        DiaryResponse response = new DiaryResponse();
        response.setConversationId(conversationId);
        response.setDiary(diary);
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
