package com.chimaenono.dearmind.gpt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "감정 기반 대화 생성 응답 DTO")
public class ConversationGenerateResponse {
    
    @Schema(description = "생성된 AI 응답", example = "정말 속상하셨을 것 같아요. 열심히 공부하신 만큼 더 아쉬우셨겠어요.")
    private String aiResponse;
    
    @Schema(description = "사용된 감정 정보", example = "슬픔 (95%)")
    private String emotionInfo;
    
    @Schema(description = "대화 메시지 ID", example = "123")
    private Long conversationMessageId;
    
    @Schema(description = "저장된 AI 메시지 ID", example = "456")
    private Long savedAIMessageId;
    
    @Schema(description = "TTS 오디오 데이터 (Base64 인코딩)", example = "UklGRnoGAABXQVZFZm10IBAAAAABAAEA...")
    private String audioBase64;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    @Schema(description = "응답 메시지", example = "AI 응답이 성공적으로 생성되었습니다.")
    private String message;
    
    public static ConversationGenerateResponse success(String aiResponse, String emotionInfo, Long conversationMessageId, Long savedAIMessageId, String audioBase64) {
        ConversationGenerateResponse response = new ConversationGenerateResponse();
        response.setAiResponse(aiResponse);
        response.setEmotionInfo(emotionInfo);
        response.setConversationMessageId(conversationMessageId);
        response.setSavedAIMessageId(savedAIMessageId);
        response.setAudioBase64(audioBase64);
        response.setSuccess(true);
        response.setMessage("AI 응답이 성공적으로 생성되고 저장되었습니다.");
        return response;
    }
    
    public static ConversationGenerateResponse success(String aiResponse, String emotionInfo, Long conversationMessageId, Long savedAIMessageId) {
        ConversationGenerateResponse response = new ConversationGenerateResponse();
        response.setAiResponse(aiResponse);
        response.setEmotionInfo(emotionInfo);
        response.setConversationMessageId(conversationMessageId);
        response.setSavedAIMessageId(savedAIMessageId);
        response.setSuccess(true);
        response.setMessage("AI 응답이 성공적으로 생성되고 저장되었습니다.");
        return response;
    }
    
    public static ConversationGenerateResponse success(String aiResponse, String emotionInfo, Long conversationMessageId) {
        ConversationGenerateResponse response = new ConversationGenerateResponse();
        response.setAiResponse(aiResponse);
        response.setEmotionInfo(emotionInfo);
        response.setConversationMessageId(conversationMessageId);
        response.setSuccess(true);
        response.setMessage("AI 응답이 성공적으로 생성되었습니다.");
        return response;
    }
    
    public static ConversationGenerateResponse error(String message) {
        ConversationGenerateResponse response = new ConversationGenerateResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
