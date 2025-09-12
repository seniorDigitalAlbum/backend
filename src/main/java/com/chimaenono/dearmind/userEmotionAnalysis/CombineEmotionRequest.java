package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 감정 계산 요청 DTO")
public class CombineEmotionRequest {
    
    @NotNull(message = "대화 메시지 ID는 필수입니다")
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
}
