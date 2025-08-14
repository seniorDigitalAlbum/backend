package com.chimaenono.dearmind.stt;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "STT 응답 DTO")
public class STTResponse {
    
    @Schema(description = "변환된 텍스트", example = "안녕하세요, 오늘 날씨가 정말 좋네요.")
    private String text;
    
    @Schema(description = "언어", example = "korean")
    private String language;
    
    @Schema(description = "신뢰도", example = "0.95")
    private Double confidence;
    
    @Schema(description = "처리 시간 (초)", example = "2.5")
    private Double duration;
    
    @Schema(description = "상태", example = "success")
    private String status;
    
    @Schema(description = "오류 메시지 (있는 경우)")
    private String error;
} 