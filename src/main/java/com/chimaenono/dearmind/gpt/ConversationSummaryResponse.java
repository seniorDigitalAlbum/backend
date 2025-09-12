package com.chimaenono.dearmind.gpt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 내용 요약 응답 DTO")
public class ConversationSummaryResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "요약된 대화 내용", example = "사용자가 어린 시절 추억을 이야기하며 그네를 타고 하늘을 보던 순간의 기쁨을 표현했습니다.")
    private String summary;
    
    @Schema(description = "요약 길이 (문자 수)", example = "45")
    private Integer summaryLength;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    @Schema(description = "응답 메시지", example = "대화 내용이 성공적으로 요약되었습니다.")
    private String message;
    
    public static ConversationSummaryResponse success(Long conversationId, String summary, Integer summaryLength) {
        ConversationSummaryResponse response = new ConversationSummaryResponse();
        response.setConversationId(conversationId);
        response.setSummary(summary);
        response.setSummaryLength(summaryLength);
        response.setSuccess(true);
        response.setMessage("대화 내용이 성공적으로 요약되었습니다.");
        return response;
    }
    
    public static ConversationSummaryResponse error(String message) {
        ConversationSummaryResponse response = new ConversationSummaryResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
