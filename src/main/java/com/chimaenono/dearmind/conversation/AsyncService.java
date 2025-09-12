package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.chimaenono.dearmind.gpt.GPTService;

@Service
@Tag(name = "Async Service", description = "비동기 처리 서비스")
public class AsyncService {
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private GPTService gptService;
    
    @Operation(summary = "대화 요약 및 일기 생성", description = "백그라운드에서 대화 요약과 일기를 생성합니다")
    @Async
    public void generateSummaryAndDiary(Long conversationId) {
        try {
            // 처리 상태를 PROCESSING으로 변경
            conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.PROCESSING);
            
            // 1. 대화 내용 요약 생성 및 저장
            String summary = gptService.generateAndSaveConversationSummary(conversationId, 50);
            
            // 2. 일기 생성 및 저장 (임시 비활성화)
            try {
                gptService.generateAndSaveDiary(conversationId, summary);
            } catch (Exception diaryException) {
                System.err.println("일기 생성 중 오류 발생: " + diaryException.getMessage());
                diaryException.printStackTrace();
                
                // 일기 생성 실패 시에도 처리 완료로 표시
                conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.COMPLETED);
                return;
            }
            
            // 3. 처리 상태를 COMPLETED로 변경
            conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.COMPLETED);
            
        } catch (Exception e) {
            // 오류 발생 시 상태를 ERROR로 변경
            conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.ERROR);
            System.err.println("=== 대화 요약 및 일기 생성 중 오류 발생 ===");
            System.err.println("Conversation ID: " + conversationId);
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류 타입: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("==========================================");
        }
    }
}
