package com.chimaenono.dearmind.album;

import com.chimaenono.dearmind.conversation.Conversation;
import com.chimaenono.dearmind.conversation.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumPhotoService {

    private final AlbumPhotoRepository albumPhotoRepository;
    private final ConversationRepository conversationRepository;

    /**
     * 특정 대화의 사진 목록을 조회합니다.
     */
    public List<AlbumPhoto> getPhotosByConversationId(Long conversationId) {
        log.info("대화 ID {}의 사진 목록 조회", conversationId);
        return albumPhotoRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    /**
     * 특정 대화의 앨범 표지 사진을 조회합니다.
     */
    public Optional<AlbumPhoto> getCoverPhotoByConversationId(Long conversationId) {
        log.info("대화 ID {}의 앨범 표지 사진 조회", conversationId);
        return albumPhotoRepository.findByConversationIdAndIsCoverTrue(conversationId);
    }

    /**
     * 새로운 사진을 추가합니다.
     */
    @Transactional
    public AlbumPhoto addPhoto(Long conversationId, String imageUrl, String uploadedBy) {
        log.info("대화 ID {}에 사진 추가: 업로더={}, URL={}", conversationId, uploadedBy, imageUrl);

        // 대화 존재 여부 확인
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId));

        // 사진 생성 및 저장
        AlbumPhoto photo = AlbumPhoto.builder()
                .conversation(conversation)
                .imageUrl(imageUrl)
                .uploadedBy(uploadedBy)
                .isCover(false) // 기본값은 표지가 아님
                .build();

        AlbumPhoto savedPhoto = albumPhotoRepository.save(photo);
        log.info("사진 추가 완료: ID={}", savedPhoto.getId());

        return savedPhoto;
    }

    /**
     * 특정 사진을 앨범 표지로 설정합니다.
     */
    @Transactional
    public void setCoverPhoto(Long conversationId, Long photoId) {
        log.info("대화 ID {}의 사진 ID {}를 앨범 표지로 설정", conversationId, photoId);

        // 해당 대화의 모든 표지 사진을 false로 변경
        albumPhotoRepository.clearCoverPhotosByConversationId(conversationId);

        // 선택된 사진을 표지로 설정 (대화 ID로 직접 조회하여 LAZY 로딩 문제 방지)
        AlbumPhoto photo = albumPhotoRepository.findByIdAndConversationId(photoId, conversationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대화에 존재하지 않는 사진입니다: conversationId=" + conversationId + ", photoId=" + photoId));

        photo.setIsCover(true);
        albumPhotoRepository.save(photo);
        log.info("앨범 표지 설정 완료: 대화 ID={}, 사진 ID={}", conversationId, photoId);
    }

    /**
     * 사진을 삭제합니다.
     */
    @Transactional
    public void deletePhoto(Long photoId) {
        log.info("사진 삭제: ID={}", photoId);
        
        AlbumPhoto photo = albumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사진입니다: " + photoId));

        albumPhotoRepository.delete(photo);
        log.info("사진 삭제 완료: ID={}", photoId);
    }

    /**
     * 특정 대화의 모든 사진을 삭제합니다.
     */
    @Transactional
    public void deletePhotosByConversationId(Long conversationId) {
        log.info("대화 ID {}의 모든 사진 삭제", conversationId);
        albumPhotoRepository.deleteByConversationId(conversationId);
        log.info("대화 ID {}의 모든 사진 삭제 완료", conversationId);
    }

    /**
     * 특정 대화의 사진 개수를 조회합니다.
     */
    public long getPhotoCountByConversationId(Long conversationId) {
        return albumPhotoRepository.countByConversationId(conversationId);
    }

    /**
     * 특정 사용자가 업로드한 사진 목록을 조회합니다.
     */
    public List<AlbumPhoto> getPhotosByUploadedBy(String uploadedBy) {
        log.info("업로더 {}의 사진 목록 조회", uploadedBy);
        return albumPhotoRepository.findByUploadedByOrderByCreatedAtDesc(uploadedBy);
    }

    /**
     * 특정 대화에서 표지 사진이 아닌 사진들을 조회합니다.
     */
    public List<AlbumPhoto> getNonCoverPhotosByConversationId(Long conversationId) {
        log.info("대화 ID {}의 표지가 아닌 사진 목록 조회", conversationId);
        return albumPhotoRepository.findByConversationIdAndIsCoverFalseOrderByCreatedAtDesc(conversationId);
    }
}
