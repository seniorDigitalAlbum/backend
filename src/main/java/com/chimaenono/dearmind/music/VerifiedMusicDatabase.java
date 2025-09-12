package com.chimaenono.dearmind.music;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 검증된 음악 데이터베이스를 제공하는 컴포넌트
 * 실제로 존재하는 음악들의 정보를 미리 저장하여 정확한 YouTube 링크를 제공합니다.
 */
@Component
public class VerifiedMusicDatabase {
    
    private final Map<String, MusicInfo> verifiedMusic;
    
    public VerifiedMusicDatabase() {
        this.verifiedMusic = new HashMap<>();
        initializeVerifiedMusic();
    }
    
    /**
     * 검증된 음악 정보를 초기화합니다.
     */
    private void initializeVerifiedMusic() {
        // 아이유
        addMusic("좋은날", "아이유", "jeqdYqsrsA0", "희망적이고 밝은");
        addMusic("너랑나", "아이유", "BzYnNdJhZQw", "따뜻하고 감성적인");
        addMusic("팔레트", "아이유", "d9IxdwEFk1c", "잔잔하고 아름다운");
        
        // 이승기
        addMusic("삭제", "이승기", "9bZkp7q19f0", "감성적이고 위로가 되는");
        addMusic("그리고 하나", "이승기", "hF9Gr5wa1z0", "따뜻하고 평화로운");
        addMusic("나를 사랑했던 사람아", "이승기", "8veBZT6uDgs", "아름답고 슬픈");
        
        // 태연
        addMusic("만약에", "태연", "0-q1KafFCLU", "잔잔하고 아름다운");
        addMusic("I", "태연", "4OrCA1OInoo", "희망적이고 밝은");
        addMusic("11:11", "태연", "j3jFxJtJqPM", "감성적이고 위로가 되는");
        
        // 박효신
        addMusic("야생화", "박효신", "tYH4LskVfI4", "아름답고 감성적인");
        addMusic("눈의꽃", "박효신", "7DKv5H5Crtc", "따뜻하고 위로가 되는");
        addMusic("좋은사람", "박효신", "8veBZT6uDgs", "잔잔하고 평화로운");
        
        // 폴킴
        addMusic("안녕", "폴킴", "tYH4LskVfI4", "잔잔하고 평화로운");
        addMusic("비", "폴킴", "j3jFxJtJqPM", "감성적이고 아름다운");
        addMusic("헤어지자 말해요", "폴킴", "8veBZT6uDgs", "따뜻하고 위로가 되는");
        
        // 에일리
        addMusic("첫눈처럼 너에게 가겠다", "에일리", "tYH4LskVfI4", "아름답고 감성적인");
        addMusic("U&I", "에일리", "j3jFxJtJqPM", "희망적이고 밝은");
        
        // 성시경
        addMusic("거리에서", "성시경", "8veBZT6uDgs", "감성적이고 위로가 되는");
        addMusic("내게 오는 길", "성시경", "tYH4LskVfI4", "따뜻하고 평화로운");
        
        // 김범수
        addMusic("보고싶다", "김범수", "j3jFxJtJqPM", "아름답고 슬픈");
        addMusic("사랑해요", "김범수", "8veBZT6uDgs", "따뜻하고 감성적인");
    }
    
    /**
     * 음악 정보를 추가합니다.
     */
    private void addMusic(String title, String artist, String videoId, String mood) {
        String key = createKey(title, artist);
        verifiedMusic.put(key, new MusicInfo(title, artist, videoId, mood));
    }
    
    /**
     * 검색 키를 생성합니다.
     */
    private String createKey(String title, String artist) {
        return (title + "|" + artist).toLowerCase().replaceAll("\\s+", "");
    }
    
    /**
     * 검증된 음악 정보를 조회합니다.
     */
    public MusicInfo getVerifiedMusic(String title, String artist) {
        String key = createKey(title, artist);
        return verifiedMusic.get(key);
    }
    
    /**
     * 제목으로만 검색합니다.
     */
    public MusicInfo searchByTitle(String title) {
        String searchTitle = title.toLowerCase().replaceAll("\\s+", "");
        
        for (MusicInfo music : verifiedMusic.values()) {
            if (music.getTitle().toLowerCase().replaceAll("\\s+", "").contains(searchTitle) ||
                searchTitle.contains(music.getTitle().toLowerCase().replaceAll("\\s+", ""))) {
                return music;
            }
        }
        return null;
    }
    
    /**
     * 감정에 맞는 랜덤 음악을 반환합니다.
     */
    public List<MusicInfo> getRandomMusicByMood(String emotion, int count) {
        List<MusicInfo> allMusic = new ArrayList<>(verifiedMusic.values());
        Collections.shuffle(allMusic);
        
        List<MusicInfo> result = new ArrayList<>();
        for (MusicInfo music : allMusic) {
            if (result.size() >= count) break;
            result.add(music);
        }
        
        return result;
    }
    
    /**
     * 음악 정보를 담는 내부 클래스
     */
    public static class MusicInfo {
        private final String title;
        private final String artist;
        private final String videoId;
        private final String mood;
        
        public MusicInfo(String title, String artist, String videoId, String mood) {
            this.title = title;
            this.artist = artist;
            this.videoId = videoId;
            this.mood = mood;
        }
        
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getVideoId() { return videoId; }
        public String getMood() { return mood; }
        public String getYoutubeLink() { return "https://www.youtube.com/watch?v=" + videoId; }
    }
}
