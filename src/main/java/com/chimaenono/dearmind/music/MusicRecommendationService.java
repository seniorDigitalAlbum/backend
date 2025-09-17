package com.chimaenono.dearmind.music;

import com.chimaenono.dearmind.gpt.GPTService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Service
@Tag(name = "Music Recommendation Service", description = "음악 추천 서비스")
public class MusicRecommendationService {
    
    @Autowired
    private MusicRecommendationRepository musicRecommendationRepository;
    
    @Autowired
    private GPTService gptService;
    
    @Autowired
    private YouTubeSearchService youtubeSearchService;
    
    @Autowired
    private VerifiedMusicDatabase verifiedMusicDatabase;
    
    @Operation(summary = "음악 추천 조회 또는 생성", description = "기존 음악 추천이 있으면 조회하고, 없으면 새로 생성합니다")
    public List<MusicRecommendation> getOrGenerateMusicRecommendations(
            Long conversationId, 
            com.chimaenono.dearmind.diary.DiaryPlan diaryPlan, 
            com.chimaenono.dearmind.diary.Summary summary) {
        
        try {
            // 1. 기존 추천 음악 조회
            List<MusicRecommendation> existing = musicRecommendationRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
            
            if (!existing.isEmpty()) {
                System.out.println("기존 음악 추천을 반환합니다. Conversation ID: " + conversationId);
                return existing; // 이미 추천된 음악이 있으면 반환
            }
            
            System.out.println("새로운 음악 추천을 생성합니다. Conversation ID: " + conversationId);
            
            // 2. GPT로 음악 추천 생성 (링크 없이)
            List<MusicRecommendation> newRecommendations = 
                gptService.generateMusicRecommendations(diaryPlan, summary);
            
            // 3. 검증된 데이터베이스에서 정확한 링크 찾기 및 저장
            for (MusicRecommendation music : newRecommendations) {
                music.setConversationId(conversationId);
                
                // 검증된 데이터베이스에서 정확한 음악 정보 찾기
                VerifiedMusicDatabase.MusicInfo verifiedMusic = 
                    verifiedMusicDatabase.getVerifiedMusic(music.getTitle(), music.getArtist());
                
                if (verifiedMusic != null) {
                    // 검증된 음악 정보 사용
                    music.setYoutubeLink(verifiedMusic.getYoutubeLink());
                    music.setYoutubeVideoId(verifiedMusic.getVideoId());
                    music.setMood(verifiedMusic.getMood()); // 더 정확한 분위기 정보
                    System.out.println("검증된 음악 사용: " + music.getTitle() + " - " + verifiedMusic.getYoutubeLink());
                } else {
                    // 검증된 데이터베이스에 없으면 제목으로 검색
                    VerifiedMusicDatabase.MusicInfo titleMatch = 
                        verifiedMusicDatabase.searchByTitle(music.getTitle());
                    
                    if (titleMatch != null) {
                        music.setTitle(titleMatch.getTitle());
                        music.setArtist(titleMatch.getArtist());
                        music.setYoutubeLink(titleMatch.getYoutubeLink());
                        music.setYoutubeVideoId(titleMatch.getVideoId());
                        music.setMood(titleMatch.getMood());
                        System.out.println("제목 매칭으로 검증된 음악 사용: " + titleMatch.getTitle() + " - " + titleMatch.getYoutubeLink());
                    } else {
                        // 최종적으로 YouTube 검색 시도
                        String youtubeLink = youtubeSearchService.searchAndGenerateLink(
                            music.getTitle(), music.getArtist());
                        
                        if (youtubeLink != null) {
                            music.setYoutubeLink(youtubeLink);
                            String videoId = youtubeSearchService.extractVideoId(youtubeLink);
                            music.setYoutubeVideoId(videoId);
                            System.out.println("YouTube 검색 성공: " + music.getTitle() + " - " + youtubeLink);
                        } else {
                            // 최종 실패 시 검증된 랜덤 음악 사용
                            List<VerifiedMusicDatabase.MusicInfo> randomMusic = 
                                verifiedMusicDatabase.getRandomMusicByMood("차분한", 1);
                            
                            if (!randomMusic.isEmpty()) {
                                VerifiedMusicDatabase.MusicInfo fallback = randomMusic.get(0);
                                music.setTitle(fallback.getTitle());
                                music.setArtist(fallback.getArtist());
                                music.setYoutubeLink(fallback.getYoutubeLink());
                                music.setYoutubeVideoId(fallback.getVideoId());
                                music.setMood(fallback.getMood());
                                System.out.println("대체 음악 사용: " + fallback.getTitle() + " - " + fallback.getYoutubeLink());
                            } else {
                                // 최종 대체
                                music.setYoutubeLink("https://www.youtube.com/results?search_query=" + 
                                    music.getTitle() + "+" + music.getArtist());
                                music.setYoutubeVideoId(null);
                                System.out.println("최종 대체 링크 사용: " + music.getTitle());
                            }
                        }
                    }
                }
                
                musicRecommendationRepository.save(music);
            }
            
            System.out.println("음악 추천이 생성되고 저장되었습니다. 개수: " + newRecommendations.size());
            return newRecommendations;
            
        } catch (Exception e) {
            System.err.println("음악 추천 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // 빈 리스트 반환
        }
    }
    
    @Operation(summary = "대화별 음악 추천 조회", description = "특정 대화의 음악 추천 목록을 조회합니다")
    public List<MusicRecommendation> getMusicRecommendationsByConversationId(Long conversationId) {
        return musicRecommendationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
    
    @Operation(summary = "대화별 음악 추천 삭제", description = "특정 대화의 음악 추천을 삭제합니다")
    public void deleteMusicRecommendationsByConversationId(Long conversationId) {
        musicRecommendationRepository.deleteByConversationId(conversationId);
    }
}
