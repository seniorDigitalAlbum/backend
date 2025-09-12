package com.chimaenono.dearmind.music;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRecommendationRepository extends JpaRepository<MusicRecommendation, Long> {
    
    /**
     * 대화 세션별 음악 추천 목록 조회 (생성 시간순)
     */
    List<MusicRecommendation> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    /**
     * 대화 세션별 음악 추천 개수 조회
     */
    long countByConversationId(Long conversationId);
    
    /**
     * 대화 세션의 음악 추천 삭제
     */
    void deleteByConversationId(Long conversationId);
}
