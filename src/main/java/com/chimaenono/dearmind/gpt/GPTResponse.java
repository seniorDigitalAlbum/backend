package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GPT API 응답 DTO")
public class GPTResponse {
    
    @Schema(description = "응답 ID", example = "chatcmpl-123")
    private String id;
    
    @Schema(description = "응답 객체 타입", example = "chat.completion")
    private String object;
    
    @Schema(description = "생성 시간", example = "1677652288")
    private Long created;
    
    @Schema(description = "사용된 모델", example = "gpt-3.5-turbo")
    private String model;
    
    @Schema(description = "선택된 선택지")
    private List<GPTChoice> choices;
    
    @Schema(description = "사용량 정보")
    private GPTUsage usage;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "GPT 선택지")
    public static class GPTChoice {
        @Schema(description = "선택지 인덱스", example = "0")
        private Integer index;
        
        @Schema(description = "메시지")
        private GPTMessage message;
        
        @Schema(description = "완료 이유", example = "stop")
        private String finishReason;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "GPT 사용량 정보")
    public static class GPTUsage {
        @Schema(description = "프롬프트 토큰 수", example = "10")
        private Integer promptTokens;
        
        @Schema(description = "완료 토큰 수", example = "20")
        private Integer completionTokens;
        
        @Schema(description = "총 토큰 수", example = "30")
        private Integer totalTokens;
    }
}
