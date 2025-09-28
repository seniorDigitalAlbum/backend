package com.chimaenono.dearmind.microphone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "발화 시작 요청 DTO")
public class SpeechStartRequest {
    
    @NotBlank(message = "마이크 세션 ID는 필수입니다")
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456", required = true)
    private String microphoneSessionId;
    
    @NotBlank(message = "카메라 세션 ID는 필수입니다")
    @Schema(description = "카메라 세션 ID", example = "camera_session_789", required = true)
    private String cameraSessionId;
    
    @Schema(description = "대화 세션 ID (선택적)", example = "1")
    private Long conversationId;
}
