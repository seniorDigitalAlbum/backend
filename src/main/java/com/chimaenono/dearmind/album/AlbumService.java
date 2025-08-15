package com.chimaenono.dearmind.album;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Tag(name = "Album Service", description = "앨범 관리 서비스")
public class AlbumService {
    
    @Autowired
    private AlbumRepository albumRepository;
    
    @Operation(summary = "앨범 생성", description = "새로운 앨범을 생성합니다")
    public Album createAlbum(String userId, Long conversationId, String finalEmotion, String diaryContent) {
        Album album = new Album();
        album.setUserId(userId);
        album.setConversationId(conversationId);
        album.setFinalEmotion(finalEmotion);
        album.setDiaryContent(diaryContent);
        album.setCreatedAt(LocalDateTime.now());
        album.setUpdatedAt(LocalDateTime.now());
        return albumRepository.save(album);
    }
    
    @Operation(summary = "앨범 조회", description = "ID로 앨범을 조회합니다")
    public Optional<Album> getAlbumById(Long albumId) {
        return albumRepository.findById(albumId);
    }
    
    @Operation(summary = "사용자별 앨범 목록 조회", description = "사용자의 모든 앨범을 최신순으로 조회합니다")
    public List<Album> getAlbumsByUserId(String userId) {
        return albumRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Operation(summary = "대화 세션별 앨범 조회", description = "대화 세션 ID로 앨범을 조회합니다")
    public Optional<Album> getAlbumByConversationId(Long conversationId) {
        return albumRepository.findByConversationId(conversationId);
    }
    
    @Operation(summary = "사용자의 앨범 개수 조회", description = "사용자가 가진 앨범의 총 개수를 조회합니다")
    public long getAlbumCountByUserId(String userId) {
        return albumRepository.countByUserId(userId);
    }
    
    @Operation(summary = "앨범 업데이트", description = "앨범 정보를 업데이트합니다")
    public Album updateAlbum(Long albumId, String finalEmotion, String diaryContent) {
        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            Album album = optionalAlbum.get();
            album.setFinalEmotion(finalEmotion);
            album.setDiaryContent(diaryContent);
            album.setUpdatedAt(LocalDateTime.now());
            return albumRepository.save(album);
        }
        return null;
    }
    
    @Operation(summary = "앨범 삭제", description = "앨범을 삭제합니다")
    public boolean deleteAlbum(Long albumId) {
        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            albumRepository.deleteById(albumId);
            return true;
        }
        return false;
    }
    
    @Operation(summary = "감정별 앨범 조회", description = "특정 감정의 앨범들을 조회합니다")
    public List<Album> getAlbumsByEmotion(String userId, String emotion) {
        return albumRepository.findByUserIdAndFinalEmotion(userId, emotion);
    }
    
    @Operation(summary = "더미 앨범 데이터 생성", description = "테스트용 더미 앨범 데이터를 생성합니다")
    public void createDummyAlbums(String userId) {
        // 기존 더미 데이터가 있으면 생성하지 않음
        if (albumRepository.countByUserId(userId) > 0) {
            return;
        }
        
        // 더미 데이터 생성
        String[] emotions = {"기쁨", "그리움", "감사함", "평온함", "설렘"};
        String[] diaryContents = {
            "오늘은 어린 시절 추억에 대해 이야기하며 따뜻한 기분을 느꼈습니다. 특히 놀이터에서 친구들과 함께 놀던 기억이 가장 인상 깊었고, 그때의 순수한 기쁨을 다시 한번 느낄 수 있어서 좋았습니다.",
            "가족과 함께한 추억을 떠올리며 그리운 마음이 들었습니다. 어머니가 해주시던 음식의 맛, 아버지와 함께한 등산, 형제들과의 장난 등 모든 것이 소중하게 느껴졌습니다.",
            "첫사랑에 대한 이야기를 나누며 감사한 마음이 들었습니다. 그때의 순수한 마음과 설렘, 그리고 아름다운 추억들이 지금도 내 마음속에 살아있다는 것을 깨달았습니다.",
            "직장에서의 첫 경험을 이야기하며 평온한 마음이 들었습니다. 긴장했던 순간들, 성취감을 느꼈던 순간들, 동료들과의 우정 등 모든 것이 소중한 경험이었다는 것을 알게 되었습니다.",
            "해외여행 경험을 이야기하며 설렘을 느꼈습니다. 새로운 곳을 탐험하는 기쁨, 다른 문화를 경험하는 즐거움, 그리고 그때의 자유로웠던 마음이 다시 떠올랐습니다."
        };
        
        for (int i = 0; i < 5; i++) {
            Album album = new Album();
            album.setUserId(userId);
            album.setConversationId((long) (i + 1));
            album.setFinalEmotion(emotions[i]);
            album.setDiaryContent(diaryContents[i]);
            album.setCreatedAt(LocalDateTime.now().minusDays(i));
            album.setUpdatedAt(LocalDateTime.now().minusDays(i));
            albumRepository.save(album);
        }
    }
} 