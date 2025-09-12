package com.chimaenono.dearmind.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "처리 상태 응답 DTO")
public class ProcessingStatusResponse {
    
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "처리 상태", example = "COMPLETED", allowableValues = {"READY", "PROCESSING", "COMPLETED", "ERROR"})
    private String status;
    
    @Schema(description = "요약 생성 완료 여부", example = "true")
    private Boolean summaryCompleted;
    
    @Schema(description = "일기 생성 완료 여부", example = "true")
    private Boolean diaryCompleted;
    
    @Schema(description = "응답 메시지", example = "처리가 완료되었습니다.")
    private String message;
    
    @Schema(description = "성공 여부", example = "true")
    private Boolean success;
    
    public static ProcessingStatusResponse success(Long conversationId, String status, Boolean summaryCompleted, 
                                                 Boolean diaryCompleted, String message) {
        ProcessingStatusResponse response = new ProcessingStatusResponse();
        response.setConversationId(conversationId);
        response.setStatus(status);
        response.setSummaryCompleted(summaryCompleted);
        response.setDiaryCompleted(diaryCompleted);
        response.setMessage(message);
        response.setSuccess(true);
        return response;
    }
    
    public static ProcessingStatusResponse error(String message) {
        ProcessingStatusResponse response = new ProcessingStatusResponse();
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
}
