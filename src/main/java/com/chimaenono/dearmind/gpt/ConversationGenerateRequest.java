package com.chimaenono.dearmind.gpt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "감정 기반 대화 생성 요청 DTO")
public class ConversationGenerateRequest {
    
    @NotNull(message = "대화 메시지 ID는 필수입니다")
    @Schema(description = "대화 메시지 ID", example = "123", required = true)
    private Long conversationMessageId;
    
    @Schema(description = "사용자 감정", example = "슬픔", allowableValues = {"기쁨", "당황", "분노", "불안", "상처", "슬픔", "중립"})
    private String emotion;
    
    @Schema(description = "감정 신뢰도", example = "0.95")
    private Double confidence;
    
    @Schema(description = "이전 사용자 발화", example = "오늘 중요한 시험을 망친 것 같아요. 너무 속상해요.")
    private String prevUser;
    
    @Schema(description = "이전 AI 발화", example = "아이고, 정말 힘들었겠어요. 수고 많으셨어요.")
    private String prevSys;
    
    @Schema(description = "현재 사용자 발화", example = "공부한 만큼 결과가 안 나와서 실망이 커요.")
    private String currUser;
}
