package com.chimaenono.dearmind.album;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumCommentRepository extends JpaRepository<AlbumComment, Long> {

    /**
     * 특정 대화의 댓글 목록을 생성일 내림차순으로 조회합니다.
     */
    @Query("SELECT ac FROM AlbumComment ac WHERE ac.conversationId = :conversationId ORDER BY ac.createdAt DESC")
    List<AlbumComment> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 댓글 목록을 페이지네이션으로 조회합니다.
     */
    @Query("SELECT ac FROM AlbumComment ac WHERE ac.conversationId = :conversationId ORDER BY ac.createdAt DESC")
    Page<AlbumComment> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * 특정 대화의 댓글 개수를 조회합니다.
     */
    @Query("SELECT COUNT(ac) FROM AlbumComment ac WHERE ac.conversationId = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 댓글을 모두 삭제합니다.
     */
    @Query("DELETE FROM AlbumComment ac WHERE ac.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 사용자가 작성한 댓글 목록을 조회합니다.
     */
    @Query("SELECT ac FROM AlbumComment ac WHERE ac.userId = :userId ORDER BY ac.createdAt DESC")
    List<AlbumComment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
