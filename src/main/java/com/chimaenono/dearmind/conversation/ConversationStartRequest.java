package com.chimaenono.dearmind.conversation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대화 시작 요청 DTO
 * 카메라 세션, 마이크 세션, 대화방을 통합으로 생성하기 위한 요청 데이터
 */
@Schema(description = "대화 시작 요청")
public class ConversationStartRequest {
    
    @Schema(description = "선택한 질문 ID", example = "5", required = true)
    private Long questionId;
    
    // 기본 생성자
    public ConversationStartRequest() {}
    
    // 전체 생성자
    public ConversationStartRequest(Long questionId) {
        this.questionId = questionId;
    }
    
    // Getter와 Setter
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    @Override
    public String toString() {
        return "ConversationStartRequest{" +
                "questionId=" + questionId +
                '}';
    }
}
