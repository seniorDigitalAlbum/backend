package com.chimaenono.dearmind.conversation;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 종료 응답 DTO")
public class ConversationEndResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "대화 상태", example = "COMPLETED")
    private String status;
    
    @Schema(description = "처리 상태", example = "PROCESSING")
    private String processingStatus;
    
    @Schema(description = "대화 메시지 목록")
    private List<ConversationMessage> messages;
    
    @Schema(description = "응답 메시지", example = "일기 생성 중입니다...")
    private String message;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    public static ConversationEndResponse success(Long conversationId, String status, String processingStatus, 
                                                 List<ConversationMessage> messages, String message) {
        ConversationEndResponse response = new ConversationEndResponse();
        response.setConversationId(conversationId);
        response.setStatus(status);
        response.setProcessingStatus(processingStatus);
        response.setMessages(messages);
        response.setMessage(message);
        response.setSuccess(true);
        return response;
    }
    
    public static ConversationEndResponse error(String message) {
        ConversationEndResponse response = new ConversationEndResponse();
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
}
