package com.chimaenono.dearmind.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 컨텍스트 응답 DTO")
public class ConversationContextResponse {
    
    @Schema(description = "이전 사용자 발화", example = "면접 결과를 기다리고 있는데 연락이 없네요.", nullable = true)
    private String prevUser;
    
    @Schema(description = "이전 시스템(AI) 발화", example = "조금 더 기다려 보시는 게 어떨까요?", nullable = true)
    private String prevSys;
    
    @Schema(description = "현재 사용자 발화", example = "네, 근데 혹시 떨어졌을까 봐 너무 불안해요. 계속 초조하네요.", required = true)
    private String currUser;
    
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
    
    @Schema(description = "대화 세션 ID", example = "1", required = true)
    private Long conversationId;
    
    @Schema(description = "컨텍스트 조회 성공 여부", example = "true", required = true)
    private Boolean success;
    
    @Schema(description = "응답 메시지", example = "대화 컨텍스트를 성공적으로 조회했습니다.")
    private String message;
    
    // 정적 팩토리 메서드들
    public static ConversationContextResponse success(String prevUser, String prevSys, String currUser, 
                                                     Long conversationMessageId, Long conversationId) {
        ConversationContextResponse response = new ConversationContextResponse();
        response.setPrevUser(prevUser);
        response.setPrevSys(prevSys);
        response.setCurrUser(currUser);
        response.setConversationMessageId(conversationMessageId);
        response.setConversationId(conversationId);
        response.setSuccess(true);
        response.setMessage("대화 컨텍스트를 성공적으로 조회했습니다.");
        return response;
    }
    
    public static ConversationContextResponse error(String message) {
        ConversationContextResponse response = new ConversationContextResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
