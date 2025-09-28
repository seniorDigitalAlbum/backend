package com.chimaenono.dearmind.microphone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "발화 종료 응답 DTO")
public class SpeechEndResponse {
    
    @Schema(description = "응답 상태", example = "success")
    private String status;
    
    @Schema(description = "응답 메시지", example = "발화가 종료되었습니다.")
    private String message;
    
    @Schema(description = "생성된 대화 메시지 ID", example = "123")
    private Long conversationMessageId;
    
    @Schema(description = "사용자 발화 텍스트", example = "어릴 때 자주 했던 놀이는 숨바꼭질이었어요.")
    private String userText;
    
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456")
    private String microphoneSessionId;
    
    @Schema(description = "카메라 세션 ID", example = "camera_session_789")
    private String cameraSessionId;
    
    
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    public static SpeechEndResponse success(Long conversationMessageId, String userText, 
                                          String microphoneSessionId, String cameraSessionId, 
                                          Long conversationId) {
        return SpeechEndResponse.builder()
                .status("success")
                .message("발화가 종료되었습니다.")
                .conversationMessageId(conversationMessageId)
                .userText(userText)
                .microphoneSessionId(microphoneSessionId)
                .cameraSessionId(cameraSessionId)
                .conversationId(conversationId)
                .build();
    }
    
    public static SpeechEndResponse error(String message) {
        return SpeechEndResponse.builder()
                .status("error")
                .message(message)
                .build();
    }
}
