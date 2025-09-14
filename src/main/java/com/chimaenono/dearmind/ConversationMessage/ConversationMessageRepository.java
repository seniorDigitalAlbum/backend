package com.chimaenono.dearmind.conversationMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    
    // 대화 세션별 메시지 조회 (시간순)
    List<ConversationMessage> findByConversationIdOrderByTimestampAsc(Long conversationId);
    
    // 대화 세션별 발신자 타입별 메시지 조회
    List<ConversationMessage> findByConversationIdAndSenderTypeOrderByTimestampAsc(
        Long conversationId, ConversationMessage.SenderType senderType);
    
    // 대화 세션의 메시지 개수 조회
    long countByConversationId(Long conversationId);
    
    // 대화 세션별 발신자 타입별 메시지 개수 조회
    long countByConversationIdAndSenderType(Long conversationId, ConversationMessage.SenderType senderType);
    
    // 현재 메시지보다 이전의 사용자 메시지 중 가장 최근 것 조회
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.conversationId = :conversationId AND cm.senderType = 'USER' AND cm.id < :currentMessageId ORDER BY cm.timestamp DESC")
    List<ConversationMessage> findPreviousUserMessages(@Param("conversationId") Long conversationId, @Param("currentMessageId") Long currentMessageId, Pageable pageable);
    
    // 현재 메시지보다 이전의 AI 메시지 중 가장 최근 것 조회
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.conversationId = :conversationId AND cm.senderType = 'AI' AND cm.id < :currentMessageId ORDER BY cm.timestamp DESC")
    List<ConversationMessage> findPreviousSystemMessages(@Param("conversationId") Long conversationId, @Param("currentMessageId") Long currentMessageId, Pageable pageable);
} 