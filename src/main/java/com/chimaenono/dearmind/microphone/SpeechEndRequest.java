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
    
    @NotBlank(message = "마이크 세션 ID는 필수입니다")
    @Schema(description = "마이크 세션 ID", example = "microphone_session_456", required = true)
    private String microphoneSessionId;
    
    @NotBlank(message = "카메라 세션 ID는 필수입니다")
    @Schema(description = "카메라 세션 ID", example = "camera_session_789", required = true)
    private String cameraSessionId;
    
    @NotNull(message = "대화 세션 ID는 필수입니다")
    @Schema(description = "대화 세션 ID", example = "1", required = true)
    private Long conversationId;
    
    @NotBlank(message = "오디오 데이터는 필수입니다")
    @Schema(description = "오디오 데이터 (Base64 인코딩)", example = "base64_encoded_audio_data", required = true)
    private String audioData;
}
