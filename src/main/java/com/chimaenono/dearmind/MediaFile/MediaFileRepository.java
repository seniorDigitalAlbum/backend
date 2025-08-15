package com.chimaenono.dearmind.mediaFile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    
    // 대화 세션별 미디어 파일 조회
    List<MediaFile> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    
    // 메시지별 미디어 파일 조회
    List<MediaFile> findByMessageId(Long messageId);
    
    // 파일 타입별 미디어 파일 조회
    List<MediaFile> findByConversationIdAndFileTypeOrderByCreatedAtDesc(
        Long conversationId, MediaFile.FileType fileType);
    
    // 대화 세션의 미디어 파일 개수 조회
    long countByConversationId(Long conversationId);
} 