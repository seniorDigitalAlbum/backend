package com.chimaenono.dearmind.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    // 사용자별 대화 세션 조회
    List<Conversation> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // 사용자의 활성 대화 세션 조회
    Optional<Conversation> findByUserIdAndStatus(String userId, Conversation.ConversationStatus status);
    
    // 세션 ID로 대화 조회
    Optional<Conversation> findBySessionId(String sessionId);
    
    // 질문별 대화 세션 조회
    List<Conversation> findByQuestionIdOrderByCreatedAtDesc(Long questionId);
} 