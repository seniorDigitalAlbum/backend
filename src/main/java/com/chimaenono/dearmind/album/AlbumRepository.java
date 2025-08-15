package com.chimaenono.dearmind.album;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    
    // 사용자별 앨범 조회 (최신순)
    List<Album> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // 대화 세션별 앨범 조회
    Optional<Album> findByConversationId(Long conversationId);
    
    // 사용자의 앨범 개수 조회
    long countByUserId(String userId);
    
    // 사용자와 감정별 앨범 조회
    List<Album> findByUserIdAndFinalEmotion(String userId, String finalEmotion);
} 