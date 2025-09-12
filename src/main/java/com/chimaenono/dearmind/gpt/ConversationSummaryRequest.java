package com.chimaenono.dearmind.gpt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 내용 요약 요청 DTO")
public class ConversationSummaryRequest {
    
    @NotNull(message = "대화 세션 ID는 필수입니다")
    @Schema(description = "대화 세션 ID", example = "123", required = true)
    private Long conversationId;
    
    @Schema(description = "요약 길이 (문자 수)", example = "50", defaultValue = "50")
    private Integer summaryLength = 50;
}
