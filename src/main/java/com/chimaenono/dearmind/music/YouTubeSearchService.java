package com.chimaenono.dearmind.music;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class YouTubeSearchService {
    
    @Value("${youtube.api.key}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    
    /**
     * 음악 제목과 아티스트로 YouTube에서 검색하여 링크를 생성합니다.
     * 
     * @param title 음악 제목
     * @param artist 아티스트명
     * @return YouTube 링크 (검색 실패 시 null)
     */
    public String searchAndGenerateLink(String title, String artist) {
        try {
            // 1단계: 정확한 검색어로 시도
            String searchQuery = String.format("\"%s\" \"%s\" official", title, artist);
            String result = performSearch(searchQuery, 3);
            if (result != null && isValidMusicResult(result, title, artist)) {
                return result;
            }
            
            // 2단계: 더 구체적인 검색어로 시도
            searchQuery = String.format("\"%s\" \"%s\" music video", title, artist);
            result = performSearch(searchQuery, 5);
            if (result != null && isValidMusicResult(result, title, artist)) {
                return result;
            }
            
            // 3단계: 아티스트 채널에서 검색
            searchQuery = String.format("\"%s\" \"%s\"", title, artist);
            result = performSearch(searchQuery, 5);
            if (result != null && isValidMusicResult(result, title, artist)) {
                return result;
            }
            
            // 4단계: 제목만으로 검색
            searchQuery = String.format("\"%s\" official music", title);
            result = performSearch(searchQuery, 5);
            if (result != null && isValidMusicResult(result, title, artist)) {
                return result;
            }
            
        } catch (Exception e) {
            System.err.println("YouTube 검색 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 실제 YouTube 검색을 수행합니다.
     */
    private String performSearch(String searchQuery, int maxResults) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = String.format("%s?part=snippet&q=%s&type=video&maxResults=%d&videoCategoryId=10&key=%s", 
                YOUTUBE_SEARCH_URL, encodedQuery, maxResults, apiKey);
            
            // HTTP 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseVideoIdFromResponse(response.getBody());
            }
            
        } catch (Exception e) {
            System.err.println("YouTube 검색 수행 중 오류 발생: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 검색 결과가 유효한 음악인지 검증합니다.
     */
    private boolean isValidMusicResult(String youtubeLink, String title, String artist) {
        if (youtubeLink == null) return false;
        
        try {
            // videoId 추출
            String videoId = extractVideoId(youtubeLink);
            if (videoId == null) return false;
            
            // 비디오 정보 조회
            String videoInfo = getVideoInfo(videoId);
            if (videoInfo == null) return false;
            
            // 제목과 아티스트가 일치하는지 확인
            return isTitleAndArtistMatch(videoInfo, title, artist);
            
        } catch (Exception e) {
            System.err.println("음악 결과 검증 중 오류: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 비디오 정보를 조회합니다.
     */
    private String getVideoInfo(String videoId) {
        try {
            String url = String.format("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=%s&key=%s", 
                videoId, apiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            
        } catch (Exception e) {
            System.err.println("비디오 정보 조회 중 오류: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 비디오 제목과 아티스트가 검색어와 일치하는지 확인합니다.
     */
    private boolean isTitleAndArtistMatch(String videoInfo, String title, String artist) {
        try {
            JsonNode rootNode = objectMapper.readTree(videoInfo);
            JsonNode items = rootNode.get("items");
            
            if (items != null && items.isArray() && items.size() > 0) {
                JsonNode snippet = items.get(0).get("snippet");
                if (snippet != null) {
                    String videoTitle = snippet.get("title").asText().toLowerCase();
                    String channelTitle = snippet.get("channelTitle").asText().toLowerCase();
                    
                    // 제목과 아티스트가 포함되어 있는지 확인
                    boolean titleMatch = videoTitle.contains(title.toLowerCase()) || 
                                       title.toLowerCase().contains(videoTitle);
                    boolean artistMatch = channelTitle.contains(artist.toLowerCase()) || 
                                        videoTitle.contains(artist.toLowerCase());
                    
                    System.out.println("검증 결과 - 제목: " + videoTitle + ", 채널: " + channelTitle + 
                                     ", 제목일치: " + titleMatch + ", 아티스트일치: " + artistMatch);
                    
                    return titleMatch && artistMatch;
                }
            }
            
        } catch (Exception e) {
            System.err.println("제목/아티스트 매칭 검증 중 오류: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * YouTube API 응답에서 videoId를 추출하여 링크를 생성합니다.
     * 음악 관련 키워드가 포함된 결과를 우선적으로 선택합니다.
     * 
     * @param responseBody API 응답 JSON
     * @return YouTube 링크 또는 null
     */
    private String parseVideoIdFromResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode items = rootNode.get("items");
            
            if (items != null && items.isArray() && items.size() > 0) {
                // 여러 결과 중에서 가장 적합한 것을 선택
                for (JsonNode item : items) {
                    JsonNode snippet = item.get("snippet");
                    if (snippet != null) {
                        String title = snippet.get("title").asText().toLowerCase();
                        String description = snippet.get("description").asText().toLowerCase();
                        
                        // 음악 관련 키워드가 포함된 결과 우선 선택
                        if (isMusicRelated(title, description)) {
                            JsonNode idNode = item.get("id");
                            if (idNode != null && idNode.has("videoId")) {
                                String videoId = idNode.get("videoId").asText();
                                System.out.println("선택된 동영상: " + snippet.get("title").asText());
                                return "https://www.youtube.com/watch?v=" + videoId;
                            }
                        }
                    }
                }
                
                // 음악 관련 결과가 없으면 첫 번째 결과 사용
                JsonNode firstItem = items.get(0);
                JsonNode idNode = firstItem.get("id");
                if (idNode != null && idNode.has("videoId")) {
                    String videoId = idNode.get("videoId").asText();
                    System.out.println("첫 번째 결과 사용: " + firstItem.get("snippet").get("title").asText());
                    return "https://www.youtube.com/watch?v=" + videoId;
                }
            }
            
        } catch (Exception e) {
            System.err.println("YouTube 응답 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 제목과 설명이 음악 관련인지 확인합니다.
     */
    private boolean isMusicRelated(String title, String description) {
        String[] musicKeywords = {
            "music", "song", "음악", "노래", "mv", "music video", 
            "official", "lyrics", "가사", "뮤직비디오", "음악비디오"
        };
        
        String combined = title + " " + description;
        for (String keyword : musicKeywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 음악 제목만으로도 검색을 시도합니다 (아티스트 정보가 없는 경우).
     * 
     * @param title 음악 제목
     * @return YouTube 링크 (검색 실패 시 null)
     */
    public String searchByTitleOnly(String title) {
        return searchAndGenerateLink(title, "");
    }
    
    /**
     * YouTube 링크에서 videoId를 추출합니다.
     * 
     * @param youtubeLink YouTube 링크
     * @return videoId 또는 null
     */
    public String extractVideoId(String youtubeLink) {
        if (youtubeLink == null || youtubeLink.isEmpty()) {
            return null;
        }
        
        try {
            // 다양한 YouTube 링크 형식 지원
            if (youtubeLink.contains("youtube.com/watch?v=")) {
                String[] parts = youtubeLink.split("v=");
                if (parts.length > 1) {
                    String videoId = parts[1].split("&")[0];
                    return videoId;
                }
            } else if (youtubeLink.contains("youtu.be/")) {
                String[] parts = youtubeLink.split("youtu.be/");
                if (parts.length > 1) {
                    String videoId = parts[1].split("\\?")[0];
                    return videoId;
                }
            }
        } catch (Exception e) {
            System.err.println("VideoId 추출 중 오류 발생: " + e.getMessage());
        }
        
        return null;
    }
}
