package com.chimaenono.dearmind.conversationMessage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 메시지 생성 요청 DTO")
public class ConversationMessageRequest {
    
    @NotNull(message = "대화 ID는 필수입니다")
    @Schema(description = "대화 세션 ID", example = "1", required = true)
    private Long conversationId;
    
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Schema(description = "메시지 내용", example = "안녕하세요, 오늘 기분이 좋아요", required = true)
    private String content;
    
    @NotNull(message = "발신자 타입은 필수입니다")
    @Schema(description = "발신자 타입", example = "USER", required = true)
    private ConversationMessage.SenderType senderType;
}
