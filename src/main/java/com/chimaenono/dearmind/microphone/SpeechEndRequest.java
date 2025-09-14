package com.chimaenono.dearmind.microphone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "발화 종료 요청 DTO")
public class SpeechEndRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "user123", required = true)
    private String userId;
    
    @NotBlank(message = "마이크 세션 ID는 필수입니다")
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456", required = true)
    private String microphoneSessionId;
    
    @NotBlank(message = "카메라 세션 ID는 필수입니다")
    @Schema(description = "카메라 세션 ID", example = "camera_session_789", required = true)
    private String cameraSessionId;
    
    @NotNull(message = "대화 세션 ID는 필수입니다")
    @Schema(description = "대화 세션 ID", example = "1", required = true)
    private Long conversationId;
    
    @NotBlank(message = "사용자 발화 텍스트는 필수입니다")
    @Schema(description = "사용자 발화 텍스트 (STT 결과)", example = "어릴 때 자주 했던 놀이는 숨바꼭질이었어요.", required = true)
    private String userText;
}
