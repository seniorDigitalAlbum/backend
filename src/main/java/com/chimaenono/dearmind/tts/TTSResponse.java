package com.chimaenono.dearmind.tts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "TTS 응답 DTO")
public class TTSResponse {
    
    @Schema(description = "변환된 오디오 데이터 (Base64)", example = "UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUarm7blmGgU7k9n1unEiBC13yO/eizEIHWq+8+OWT")
    private String audioData;
    
    @Schema(description = "오디오 포맷", example = "mp3")
    private String format;
    
    @Schema(description = "음성 타입", example = "nara")
    private String voice;
    
    @Schema(description = "처리 시간 (초)", example = "1.5")
    private Double duration;
    
    @Schema(description = "상태", example = "success")
    private String status;
    
    @Schema(description = "오류 메시지", example = "텍스트 변환 실패")
    private String error;
    
    // 성공 여부 확인 메서드
    public boolean isSuccess() {
        return "success".equals(status) && audioData != null && !audioData.isEmpty();
    }
    
    // 오디오 데이터 getter (이미 @Data로 생성되지만 명시적으로 추가)
    public String getAudioData() {
        return audioData;
    }
} 