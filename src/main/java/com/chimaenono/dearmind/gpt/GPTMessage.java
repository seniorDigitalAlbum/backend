package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GPT 메시지 DTO")
public class GPTMessage {
    
    @NotBlank(message = "역할은 필수입니다")
    @Schema(description = "메시지 역할", example = "system", allowableValues = {"system", "user", "assistant"}, required = true)
    private String role;
    
    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "메시지 내용", example = "너는 사용자의 감정에 공감하는 AI 친구야.", required = true)
    private String content;
}
