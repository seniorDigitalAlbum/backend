package com.chimaenono.dearmind.album;

import com.chimaenono.dearmind.conversation.Conversation;
import com.chimaenono.dearmind.conversation.ConversationRepository;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import com.chimaenono.dearmind.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumPhotoService {

    private final AlbumPhotoRepository albumPhotoRepository;
    private final ConversationRepository conversationRepository;
    private final AlbumCommentRepository albumCommentRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 특정 대화의 사진 목록을 조회합니다.
     */
    public List<AlbumPhoto> getPhotosByConversationId(Long conversationId) {
        try {
            log.info("대화 ID {}의 사진 목록 조회 시작", conversationId);
            
            // 대화 존재 여부 확인
            if (!conversationRepository.existsById(conversationId)) {
                log.error("존재하지 않는 대화 ID: {}", conversationId);
                throw new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId);
            }
            
            List<AlbumPhoto> photos = albumPhotoRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
            log.info("대화 ID {}의 사진 {}개 조회 완료", conversationId, photos.size());
            return photos;
        } catch (Exception e) {
            log.error("사진 목록 조회 중 예외 발생: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw e;
        }
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
    public AlbumPhoto addPhoto(Long conversationId, String imageUrl, Long userId) {
        log.info("대화 ID {}에 사진 추가: 사용자ID={}, URL={}", conversationId, userId, imageUrl);

        // 대화 존재 여부 확인
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId));

        // 사진 생성 및 저장
        AlbumPhoto photo = AlbumPhoto.builder()
                .conversationId(conversationId)
                .imageUrl(imageUrl)
                .userId(userId)
                .isCover(false) // 기본값은 표지가 아님
                .build();

        AlbumPhoto savedPhoto = albumPhotoRepository.save(photo);
        log.info("사진 추가 완료: ID={}", savedPhoto.getId());

        // 대화 소유자에게 사진 업로드 알람 생성
        try {
            createPhotoUploadNotification(conversationId, userId, savedPhoto.getId());
        } catch (Exception e) {
            log.error("사진 업로드 알람 생성 실패: conversationId={}, userId={}, photoId={}, error={}", 
                    conversationId, userId, savedPhoto.getId(), e.getMessage());
            // 알람 생성 실패해도 사진 추가는 성공으로 처리
        }

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
    public List<AlbumPhoto> getPhotosByUserId(Long userId) {
        log.info("사용자 ID {}의 사진 목록 조회", userId);
        return albumPhotoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 대화에서 표지 사진이 아닌 사진들을 조회합니다.
     */
    public List<AlbumPhoto> getNonCoverPhotosByConversationId(Long conversationId) {
        log.info("대화 ID {}의 표지가 아닌 사진 목록 조회", conversationId);
        return albumPhotoRepository.findByConversationIdAndIsCoverFalseOrderByCreatedAtDesc(conversationId);
    }

    /**
     * 시니어의 최신 표지 사진을 조회합니다.
     */
    public AlbumPhoto getSeniorCoverPhoto(String seniorUserId) {
        log.info("🔍 시니어 표지 사진 조회: seniorUserId={}", seniorUserId);
        
        try {
            // 시니어의 대화 중에서 표지 사진을 조회
            List<AlbumPhoto> coverPhotos = albumPhotoRepository.findBySeniorUserIdAndIsCoverTrue(seniorUserId);
            
            if (coverPhotos.isEmpty()) {
                log.info("🔍 시니어 표지 사진 없음: seniorUserId={}", seniorUserId);
                return null;
            }
            
            // 가장 최근 표지 사진 반환
            AlbumPhoto latestCoverPhoto = coverPhotos.get(0);
            log.info("✅ 시니어 표지 사진 조회 성공: seniorUserId={}, imageUrl={}", seniorUserId, latestCoverPhoto.getImageUrl());
            return latestCoverPhoto;
        } catch (Exception e) {
            log.error("❌ 시니어 표지 사진 조회 실패: seniorUserId={}, error={}", seniorUserId, e.getMessage(), e);
            return null;
        }
    }



    /**
     * 앨범의 공개 상태를 업데이트합니다.
     */
    @Transactional
    public void updateAlbumVisibility(Long conversationId, Boolean isPublic) {
        log.info("🔍 앨범 공개 상태 업데이트: conversationId={}, isPublic={}", conversationId, isPublic);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화입니다: " + conversationId));
        
        conversation.setIsPublic(isPublic);
        conversationRepository.save(conversation);
        
        log.info("✅ 앨범 공개 상태 업데이트 완료: conversationId={}, isPublic={}", conversationId, isPublic);
    }

    /**
     * 특정 대화의 사진 목록을 작성자 정보와 함께 조회합니다.
     */
    public List<AlbumPhotoResponse> getPhotosWithAuthorInfo(Long conversationId) {
        log.info("대화 ID {}의 사진 목록을 작성자 정보와 함께 조회", conversationId);
        
        List<AlbumPhoto> photos = getPhotosByConversationId(conversationId);
        
        return photos.stream()
                .map(photo -> {
                    User author = userService.findById(photo.getUserId())
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + photo.getUserId()));
                    
                    return AlbumPhotoResponse.from(
                            photo,
                            author.getNickname(),
                            author.getProfileImageUrl()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 사진 업로드 알람을 생성합니다.
     */
    private void createPhotoUploadNotification(Long conversationId, Long uploaderId, Long photoId) {
        log.info("사진 업로드 알람 생성: conversationId={}, uploaderId={}, photoId={}", conversationId, uploaderId, photoId);
        
        // 대화 정보 조회
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("대화를 찾을 수 없습니다: " + conversationId));
        
        // 업로더 정보 조회
        User uploader = userService.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("업로더를 찾을 수 없습니다: " + uploaderId));
        
        // 대화 소유자에게만 알람 전송 (자신이 업로드한 경우 제외)
        Long conversationOwnerId = conversation.getUserId();
        if (!conversationOwnerId.equals(uploaderId)) {
            String title = "새로운 사진이 추가되었습니다";
            String content = String.format("%s님이 일기에 사진을 추가했습니다", uploader.getNickname());
            
            notificationService.createNotification(
                    conversationOwnerId,
                    uploaderId,
                    com.chimaenono.dearmind.notification.Notification.NotificationType.PHOTO_UPLOAD,
                    title,
                    content,
                    photoId
            );
            
            log.info("사진 업로드 알람 생성 완료: conversationOwnerId={}, uploaderId={}, photoId={}", 
                    conversationOwnerId, uploaderId, photoId);
        } else {
            log.info("자신이 업로드한 사진이므로 알람 생성하지 않음: conversationOwnerId={}, uploaderId={}", 
                    conversationOwnerId, uploaderId);
        }
    }
}
