package com.chimaenono.dearmind.userEmotionAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEmotionAnalysisRepository extends JpaRepository<UserEmotionAnalysis, Long> {
    
    // 특정 대화 메시지의 감정 분석 결과 조회
    Optional<UserEmotionAnalysis> findByConversationMessageId(Long conversationMessageId);
    
    // 특정 대화 세션의 모든 사용자 감정 분석 결과 조회
    List<UserEmotionAnalysis> findByConversationMessageConversationIdOrderByAnalysisTimestampAsc(Long conversationId);
    
    // 특정 대화 세션의 감정 분석 결과 개수 조회
    long countByConversationMessageConversationId(Long conversationId);
    
    // 특정 감정으로 필터링된 감정 분석 결과 조회
    List<UserEmotionAnalysis> findByCombinedEmotion(String emotion);
    
    // 신뢰도 범위로 필터링된 감정 분석 결과 조회
    List<UserEmotionAnalysis> findByCombinedConfidenceGreaterThanEqual(Double minConfidence);
}
