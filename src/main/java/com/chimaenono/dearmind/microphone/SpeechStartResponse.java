package com.chimaenono.dearmind.microphone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "발화 시작 응답 DTO")
public class SpeechStartResponse {
    
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456")
    private String microphoneSessionId;
    
    @Schema(description = "카메라 세션 ID", example = "camera_session_789")
    private String cameraSessionId;
    
    
    @Schema(description = "발화 세션 상태", example = "RECORDING")
    private String status;
    
    @Schema(description = "발화 시작 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime speechStartedAt;
    
    @Schema(description = "대화 세션 ID", example = "1")
    private Long conversationId;
    
    @Schema(description = "응답 메시지", example = "발화가 시작되었습니다.")
    private String message;
    
    // MicrophoneSession과 CameraSession 엔티티로부터 Response DTO 생성
    public static SpeechStartResponse from(MicrophoneSession microphoneSession, String cameraSessionId, Long conversationId) {
        SpeechStartResponse response = new SpeechStartResponse();
        response.setMicrophoneSessionId(microphoneSession.getSessionId());
        response.setCameraSessionId(cameraSessionId);
        response.setStatus(microphoneSession.getStatus());
        response.setSpeechStartedAt(LocalDateTime.now());
        response.setConversationId(conversationId);
        response.setMessage("발화가 시작되었습니다.");
        return response;
    }
}
