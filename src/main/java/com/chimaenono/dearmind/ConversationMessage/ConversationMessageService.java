package com.chimaenono.dearmind.conversationMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Tag(name = "ConversationMessage", description = "대화 메시지 관리 서비스")
public class ConversationMessageService {
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Operation(summary = "새로운 대화 메시지 생성", description = "STT로 변환된 텍스트를 대화 메시지로 저장")
    @Transactional
    public ConversationMessageResponse createMessage(ConversationMessageRequest request) {
        // ConversationMessage 엔티티 생성
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(request.getConversationId());
        message.setContent(request.getContent());
        message.setSenderType(request.getSenderType());
        message.setTimestamp(LocalDateTime.now());
        
        // 데이터베이스에 저장
        ConversationMessage savedMessage = conversationMessageRepository.save(message);
        
        // Response DTO로 변환하여 반환
        return ConversationMessageResponse.from(savedMessage);
    }
    
    @Operation(summary = "대화 세션의 모든 메시지 조회", description = "특정 대화 세션의 모든 메시지를 시간순으로 조회")
    public List<ConversationMessageResponse> getMessagesByConversationId(Long conversationId) {
        List<ConversationMessage> messages = conversationMessageRepository
            .findByConversationIdOrderByTimestampAsc(conversationId);
        
        return messages.stream()
            .map(ConversationMessageResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "특정 메시지 조회", description = "메시지 ID로 특정 메시지 조회")
    public Optional<ConversationMessageResponse> getMessageById(Long messageId) {
        Optional<ConversationMessage> message = conversationMessageRepository.findById(messageId);
        return message.map(ConversationMessageResponse::from);
    }
    
    @Operation(summary = "대화 세션의 사용자 메시지만 조회", description = "특정 대화 세션의 사용자 메시지만 조회")
    public List<ConversationMessageResponse> getUserMessagesByConversationId(Long conversationId) {
        List<ConversationMessage> messages = conversationMessageRepository
            .findByConversationIdAndSenderTypeOrderByTimestampAsc(
                conversationId, ConversationMessage.SenderType.USER);
        
        return messages.stream()
            .map(ConversationMessageResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "대화 세션의 AI 메시지만 조회", description = "특정 대화 세션의 AI 메시지만 조회")
    public List<ConversationMessageResponse> getAIMessagesByConversationId(Long conversationId) {
        List<ConversationMessage> messages = conversationMessageRepository
            .findByConversationIdAndSenderTypeOrderByTimestampAsc(
                conversationId, ConversationMessage.SenderType.AI);
        
        return messages.stream()
            .map(ConversationMessageResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "대화 세션의 메시지 개수 조회", description = "특정 대화 세션의 전체 메시지 개수 조회")
    public long getMessageCountByConversationId(Long conversationId) {
        return conversationMessageRepository.countByConversationId(conversationId);
    }
    
    @Operation(summary = "대화 세션의 사용자 메시지 개수 조회", description = "특정 대화 세션의 사용자 메시지 개수 조회")
    public long getUserMessageCountByConversationId(Long conversationId) {
        return conversationMessageRepository.countByConversationIdAndSenderType(
            conversationId, ConversationMessage.SenderType.USER);
    }
    
    @Operation(summary = "대화 세션의 AI 메시지 개수 조회", description = "특정 대화 세션의 AI 메시지 개수 조회")
    public long getAIMessageCountByConversationId(Long conversationId) {
        return conversationMessageRepository.countByConversationIdAndSenderType(
            conversationId, ConversationMessage.SenderType.AI);
    }
    
    @Operation(summary = "메시지 삭제", description = "특정 메시지 삭제")
    @Transactional
    public boolean deleteMessage(Long messageId) {
        if (conversationMessageRepository.existsById(messageId)) {
            conversationMessageRepository.deleteById(messageId);
            return true;
        }
        return false;
    }
    
    @Operation(summary = "AI 메시지 저장", description = "GPT가 생성한 답변을 AI 메시지로 저장")
    @Transactional
    public ConversationMessageResponse saveAIMessage(Long conversationId, String aiResponse) {
        // ConversationMessage 엔티티 생성
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setContent(aiResponse);
        message.setSenderType(ConversationMessage.SenderType.AI);
        message.setTimestamp(LocalDateTime.now());
        
        // 데이터베이스에 저장
        ConversationMessage savedMessage = conversationMessageRepository.save(message);
        
        // Response DTO로 변환하여 반환
        return ConversationMessageResponse.from(savedMessage);
    }
    
    /**
     * 특정 대화 세션의 사용자 메시지 개수를 카운트
     */
    public int countUserMessagesByConversationId(Long conversationId) {
        return (int) conversationMessageRepository.countByConversationIdAndSenderType(
            conversationId, ConversationMessage.SenderType.USER);
    }
}
