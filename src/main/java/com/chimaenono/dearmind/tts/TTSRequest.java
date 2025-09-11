package com.chimaenono.dearmind.tts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "TTS 요청 DTO")
public class TTSRequest {
    
    @Schema(description = "변환할 텍스트", example = "안녕하세요! 오늘 날씨가 정말 좋네요.")
    private String text;
    
    @Schema(description = "언어 코드", example = "ko-KR")
    private String languageCode;
    
    @Schema(description = "음성 이름", example = "ko-KR-Wavenet-A")
    private String voiceName;
    
    @Schema(description = "오디오 인코딩", example = "MP3")
    private String audioEncoding;
    
    @Schema(description = "음성 타입", example = "nara", allowableValues = {"nara", "nwoo", "dara", "bora", "vhyeri", "csy", "nsy", "nhs", "ndain", "njiyun", "njinho", "nsinu", "nara_call", "nwoo_call", "dara_call", "bora_call"})
    private String voice;
    
    @Schema(description = "음성 속도", example = "0.5", allowableValues = {"0.5", "0.75", "1.0", "1.25", "1.5"})
    private String speed;
    
    @Schema(description = "음성 톤", example = "0.0", allowableValues = {"-5.0", "-4.0", "-3.0", "-2.0", "-1.0", "0.0", "1.0", "2.0", "3.0", "4.0", "5.0"})
    private String pitch;
    
    @Schema(description = "음성 볼륨", example = "0.0", allowableValues = {"-5.0", "-4.0", "-3.0", "-2.0", "-1.0", "0.0", "1.0", "2.0", "3.0", "4.0", "5.0"})
    private String volume;
    
    @Schema(description = "오디오 포맷", example = "mp3", allowableValues = {"mp3", "wav"})
    private String format;
} 