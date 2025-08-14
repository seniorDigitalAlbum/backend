package com.chimaenono.dearmind.ConversationMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    
    // 대화 세션별 메시지 조회 (시간순 정렬)
    List<ConversationMessage> findByConversationIdOrderByTimestampAsc(Long conversationId);
    
    // 대화 세션별 발신자 타입별 메시지 조회
    List<ConversationMessage> findByConversationIdAndSenderTypeOrderByTimestampAsc(
        Long conversationId, ConversationMessage.SenderType senderType);
    
    // 대화 세션의 메시지 개수 조회
    long countByConversationId(Long conversationId);
} 