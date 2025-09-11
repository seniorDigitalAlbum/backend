package com.chimaenono.dearmind.conversationMessage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 메시지 응답 DTO")
public class ConversationMessageResponse {
    
    @Schema(description = "메시지 고유 ID", example = "1")
    private Long id;
    
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Schema(description = "발신자 타입", example = "USER")
    private ConversationMessage.SenderType senderType;
    
    @Schema(description = "메시지 내용", example = "안녕하세요, 오늘 기분이 좋아요")
    private String content;
    
    @Schema(description = "메시지 전송 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "감정 분석 결과 존재 여부", example = "true")
    private Boolean hasEmotionAnalysis;
    
    // ConversationMessage 엔티티로부터 Response DTO 생성
    public static ConversationMessageResponse from(ConversationMessage message) {
        ConversationMessageResponse response = new ConversationMessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSenderType(message.getSenderType());
        response.setContent(message.getContent());
        response.setTimestamp(message.getTimestamp());
        response.setHasEmotionAnalysis(message.getEmotionAnalysis() != null);
        return response;
    }
}
