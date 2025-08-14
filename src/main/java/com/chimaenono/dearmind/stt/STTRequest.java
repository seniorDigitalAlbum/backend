package com.chimaenono.dearmind.stt;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "STT 요청 DTO")
public class STTRequest {
    
    @Schema(description = "오디오 파일 데이터 (Base64 인코딩)", example = "UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUarm7blmGgU7k9n1unEiBC13yO/eizEIHWq+8+OWT...")
    private String audioData;
    
    @Schema(description = "오디오 파일 형식", example = "wav")
    private String format;
    
    @Schema(description = "언어 코드", example = "ko")
    private String language;
    
    @Schema(description = "모델", example = "whisper-1")
    private String model;
} 