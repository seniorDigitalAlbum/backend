package com.chimaenono.dearmind.album;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, Long> {

    /**
     * 특정 대화의 사진 목록을 생성일 내림차순으로 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId ORDER BY ap.createdAt DESC")
    List<AlbumPhoto> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 사진 목록을 페이지네이션으로 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId ORDER BY ap.createdAt DESC")
    Page<AlbumPhoto> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * 특정 대화의 앨범 표지 사진을 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId AND ap.isCover = true")
    Optional<AlbumPhoto> findByConversationIdAndIsCoverTrue(@Param("conversationId") Long conversationId);

    /**
     * 특정 ID와 대화 ID로 사진을 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.id = :id AND ap.conversationId = :conversationId")
    Optional<AlbumPhoto> findByIdAndConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 사진 개수를 조회합니다.
     */
    @Query("SELECT COUNT(ap) FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 사진을 모두 삭제합니다.
     */
    @Query("DELETE FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 사용자가 업로드한 사진 목록을 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.userId = :userId ORDER BY ap.createdAt DESC")
    List<AlbumPhoto> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 특정 대화에서 표지 사진이 아닌 사진들을 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.conversationId = :conversationId AND ap.isCover = false ORDER BY ap.createdAt DESC")
    List<AlbumPhoto> findByConversationIdAndIsCoverFalseOrderByCreatedAtDesc(@Param("conversationId") Long conversationId);

    /**
     * 특정 대화의 모든 표지 사진을 false로 변경합니다.
     */
    @Modifying
    @Query("UPDATE AlbumPhoto ap SET ap.isCover = false WHERE ap.conversationId = :conversationId")
    void clearCoverPhotosByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 시니어의 표지 사진들을 최신순으로 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap JOIN Conversation c ON ap.conversationId = c.id WHERE c.userId = :seniorUserId AND ap.isCover = true ORDER BY ap.createdAt DESC")
    List<AlbumPhoto> findBySeniorUserIdAndIsCoverTrue(@Param("seniorUserId") String seniorUserId);
}
