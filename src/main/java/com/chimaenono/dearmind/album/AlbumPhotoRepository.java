package com.chimaenono.dearmind.album;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<AlbumPhoto> findByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * 특정 대화의 사진 목록을 페이지네이션으로 조회합니다.
     */
    Page<AlbumPhoto> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 특정 대화의 앨범 표지 사진을 조회합니다.
     */
    Optional<AlbumPhoto> findByConversationIdAndIsCoverTrue(Long conversationId);

    /**
     * 특정 ID와 대화 ID로 사진을 조회합니다.
     */
    Optional<AlbumPhoto> findByIdAndConversationId(Long id, Long conversationId);

    /**
     * 특정 대화의 사진 개수를 조회합니다.
     */
    long countByConversationId(Long conversationId);

    /**
     * 특정 대화의 사진을 모두 삭제합니다.
     */
    void deleteByConversationId(Long conversationId);

    /**
     * 특정 사용자가 업로드한 사진 목록을 조회합니다.
     */
    @Query("SELECT ap FROM AlbumPhoto ap WHERE ap.uploadedBy = :uploadedBy ORDER BY ap.createdAt DESC")
    List<AlbumPhoto> findByUploadedByOrderByCreatedAtDesc(@Param("uploadedBy") String uploadedBy);

    /**
     * 특정 대화에서 표지 사진이 아닌 사진들을 조회합니다.
     */
    List<AlbumPhoto> findByConversationIdAndIsCoverFalseOrderByCreatedAtDesc(Long conversationId);

    /**
     * 특정 대화의 모든 표지 사진을 false로 변경합니다.
     */
    @Query("UPDATE AlbumPhoto ap SET ap.isCover = false WHERE ap.conversation.id = :conversationId")
    void clearCoverPhotosByConversationId(@Param("conversationId") Long conversationId);
}
