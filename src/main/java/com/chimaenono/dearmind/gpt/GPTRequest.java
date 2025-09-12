package com.chimaenono.dearmind.gpt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GPT API 요청 DTO")
public class GPTRequest {
    
    @NotBlank(message = "모델은 필수입니다")
    @Schema(description = "사용할 GPT 모델", example = "gpt-3.5-turbo", required = true)
    private String model;
    
    @NotNull(message = "메시지는 필수입니다")
    @Schema(description = "대화 메시지 목록", required = true)
    private List<GPTMessage> messages;
    
    @Schema(description = "최대 토큰 수", example = "500")
    private Integer max_tokens;
    
    @Schema(description = "온도 (창의성)", example = "0.7")
    private Double temperature;
    
    @Schema(description = "스트리밍 여부", example = "false")
    private Boolean stream;
}
