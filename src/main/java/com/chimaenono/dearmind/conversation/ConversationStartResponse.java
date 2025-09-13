package com.chimaenono.dearmind.conversation;

import com.chimaenono.dearmind.question.Question;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대화 시작 응답 DTO
 * 카메라 세션, 마이크 세션, 대화방 생성 결과를 포함한 응답 데이터
 */
@Schema(description = "대화 시작 응답")
public class ConversationStartResponse {
    
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Schema(description = "카메라 세션 ID", example = "cam_123")
    private String cameraSessionId;
    
    @Schema(description = "마이크 세션 ID", example = "mic_456")
    private String microphoneSessionId;
    
    @Schema(description = "대화 상태", example = "ACTIVE")
    private String status;
    
    @Schema(description = "선택된 질문 정보")
    private Question question;
    
    @Schema(description = "응답 메시지", example = "대화가 성공적으로 시작되었습니다.")
    private String message;
    
    // 기본 생성자
    public ConversationStartResponse() {}
    
    // 전체 생성자
    public ConversationStartResponse(Long conversationId, String cameraSessionId, 
                                   String microphoneSessionId, String status, 
                                   Question question, String message) {
        this.conversationId = conversationId;
        this.cameraSessionId = cameraSessionId;
        this.microphoneSessionId = microphoneSessionId;
        this.status = status;
        this.question = question;
        this.message = message;
    }
    
    // 정적 팩토리 메서드 - 성공 응답
    public static ConversationStartResponse success(Long conversationId, String cameraSessionId,
                                                   String microphoneSessionId, String status,
                                                   Question question) {
        return new ConversationStartResponse(
            conversationId, cameraSessionId, microphoneSessionId, status, question,
            "대화가 성공적으로 시작되었습니다."
        );
    }
    
    // 정적 팩토리 메서드 - 에러 응답
    public static ConversationStartResponse error(String message) {
        ConversationStartResponse response = new ConversationStartResponse();
        response.setMessage(message);
        return response;
    }
    
    // Getter와 Setter
    public Long getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getCameraSessionId() {
        return cameraSessionId;
    }
    
    public void setCameraSessionId(String cameraSessionId) {
        this.cameraSessionId = cameraSessionId;
    }
    
    public String getMicrophoneSessionId() {
        return microphoneSessionId;
    }
    
    public void setMicrophoneSessionId(String microphoneSessionId) {
        this.microphoneSessionId = microphoneSessionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ConversationStartResponse{" +
                "conversationId=" + conversationId +
                ", cameraSessionId='" + cameraSessionId + '\'' +
                ", microphoneSessionId='" + microphoneSessionId + '\'' +
                ", status='" + status + '\'' +
                ", question=" + question +
                ", message='" + message + '\'' +
                '}';
    }
}
