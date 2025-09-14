package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Optional;
import java.util.List;

@Service
@Tag(name = "ConversationContext", description = "대화 컨텍스트 관리 서비스")
public class ConversationContextService {
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Operation(summary = "대화 컨텍스트 조회", description = "특정 메시지의 이전 대화 컨텍스트를 조회합니다")
    public ConversationContextResponse getConversationContext(Long conversationMessageId) {
        try {
            // 1. 현재 메시지 조회
            Optional<ConversationMessage> currentMessageOpt = conversationMessageRepository.findById(conversationMessageId);
            if (!currentMessageOpt.isPresent()) {
                return ConversationContextResponse.error("대화 메시지를 찾을 수 없습니다: " + conversationMessageId);
            }
            
            ConversationMessage currentMessage = currentMessageOpt.get();
            Long conversationId = currentMessage.getConversationId();
            String currUser = currentMessage.getContent();
            
            // 2. 이전 사용자 메시지 조회 (가장 최근 1개만)
            Pageable pageable = PageRequest.of(0, 1);
            List<ConversationMessage> prevUserMessages = conversationMessageRepository
                .findPreviousUserMessages(conversationId, conversationMessageId, pageable);
            String prevUser = prevUserMessages.isEmpty() ? null : prevUserMessages.get(0).getContent();
            
            // 3. 이전 AI 메시지 조회 (가장 최근 1개만)
            List<ConversationMessage> prevSysMessages = conversationMessageRepository
                .findPreviousSystemMessages(conversationId, conversationMessageId, pageable);
            String prevSys = prevSysMessages.isEmpty() ? null : prevSysMessages.get(0).getContent();
            
            // 4. 응답 객체 생성
            return ConversationContextResponse.success(
                prevUser, 
                prevSys, 
                currUser, 
                conversationMessageId, 
                conversationId
            );
            
        } catch (Exception e) {
            return ConversationContextResponse.error("대화 컨텍스트 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Operation(summary = "대화 컨텍스트 존재 여부 확인", description = "특정 메시지에 대한 대화 컨텍스트가 존재하는지 확인합니다")
    public boolean hasConversationContext(Long conversationMessageId) {
        try {
            Optional<ConversationMessage> messageOpt = conversationMessageRepository.findById(conversationMessageId);
            if (!messageOpt.isPresent()) {
                return false;
            }
            
            ConversationMessage message = messageOpt.get();
            Long conversationId = message.getConversationId();
            
            // 이전 메시지가 하나라도 있으면 컨텍스트가 존재
            Pageable pageable = PageRequest.of(0, 1);
            List<ConversationMessage> prevUserMessages = conversationMessageRepository
                .findPreviousUserMessages(conversationId, conversationMessageId, pageable);
            List<ConversationMessage> prevSysMessages = conversationMessageRepository
                .findPreviousSystemMessages(conversationId, conversationMessageId, pageable);
            
            return !prevUserMessages.isEmpty() || !prevSysMessages.isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Operation(summary = "대화 세션의 메시지 개수 조회", description = "특정 대화 세션의 총 메시지 개수를 조회합니다")
    public long getMessageCount(Long conversationId) {
        return conversationMessageRepository.countByConversationId(conversationId);
    }
}
