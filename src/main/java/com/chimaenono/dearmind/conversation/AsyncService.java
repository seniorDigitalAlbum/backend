package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.chimaenono.dearmind.rdp.RdpService;
import com.chimaenono.dearmind.gpt.GPTService;

import java.util.Map;

@Slf4j
@Service
@Tag(name = "Async Service", description = "비동기 처리 서비스")
public class AsyncService {
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private RdpService rdpService;
    
    @Autowired
    private GPTService gptService;
    
    @Operation(summary = "RDP 추출 및 일기 생성", description = "백그라운드에서 RDP를 추출하고 일기를 생성합니다")
    @Async
    public void generateRDPAndDiary(Long conversationId) {
        try {
            // 처리 상태를 PROCESSING으로 변경
            conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.PROCESSING);
            
            // 1. RDP 완성 및 저장
            Map<String, Object> rdpData = rdpService.finalizeRdp(conversationId);
            log.info("RDP 완성 완료: conversationId={}, rdp={}", conversationId, rdpData);
            
            // 2. 일기 생성 및 저장 (RDP 기반)
            try {
                String diary = gptService.generateAndSaveDiary(conversationId);
                log.info("일기 생성 완료: conversationId={}, 길이={}", conversationId, diary.length());
                conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.COMPLETED);
            } catch (Exception diaryException) {
                log.error("일기 생성 중 오류 발생: {}", diaryException.getMessage(), diaryException);
                
                // 일기 생성 실패 시에도 처리 완료로 표시
                conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.COMPLETED);
                return;
            }
            
        } catch (Exception e) {
            // 오류 발생 시 상태를 ERROR로 변경
            conversationService.updateProcessingStatus(conversationId, Conversation.ProcessingStatus.ERROR);
            log.error("=== RDP 추출 및 일기 생성 중 오류 발생 ===", e);
            log.error("Conversation ID: {}", conversationId);
            log.error("오류 메시지: {}", e.getMessage());
        }
    }
}
