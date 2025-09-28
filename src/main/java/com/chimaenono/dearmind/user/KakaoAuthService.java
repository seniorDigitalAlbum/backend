package com.chimaenono.dearmind.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {
    
    private final RestTemplate restTemplate;
    private final UserService userService;
    
    // 카카오 앱 설정 (application.yml에서 주입)
    @Value("${security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    
    @Value("${security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;
    
    @Value("${security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;
    
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    
    /**
     * 카카오 인증 코드로 액세스 토큰 발급
     * @param code 카카오 인증 코드
     * @return 액세스 토큰
     */
    @SuppressWarnings("rawtypes")
    public String getAccessToken(String code) {
        log.info("카카오 액세스 토큰 발급 요청");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(KAKAO_TOKEN_URL, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenData = (Map<String, Object>) response.getBody();
                String accessToken = (String) tokenData.get("access_token");
                log.info("카카오 액세스 토큰 발급 성공");
                return accessToken;
            } else {
                log.error("카카오 액세스 토큰 발급 실패: {}", response.getStatusCode());
                throw new RuntimeException("카카오 액세스 토큰 발급에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 발급 중 오류 발생", e);
            throw new RuntimeException("카카오 액세스 토큰 발급 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 액세스 토큰으로 카카오 사용자 정보 조회
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보
     */
    @SuppressWarnings("rawtypes")
    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 조회 요청");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                KAKAO_USER_INFO_URL, 
                HttpMethod.GET, 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) response.getBody();
                log.info("카카오 사용자 정보 조회 성공");
                return parseKakaoUserInfo(userData);
            } else {
                log.error("카카오 사용자 정보 조회 실패: {}", response.getStatusCode());
                throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 중 오류 발생", e);
            throw new RuntimeException("카카오 사용자 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 카카오 로그인 처리 (사용자 생성 또는 업데이트)
     * @param code 카카오 인증 코드
     * @return 로그인된 사용자 정보
     */
    public User processKakaoLogin(String code) {
        log.info("카카오 로그인 처리 시작");
        
        // 1. 액세스 토큰 발급
        String accessToken = getAccessToken(code);
        
        // 2. 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(accessToken);
        
        // 3. 사용자 생성 또는 업데이트
        User user = userService.createOrUpdateUser(
            kakaoUserInfo.getId(),
            kakaoUserInfo.getNickname(),
            kakaoUserInfo.getProfileImageUrl()
        );
        
        // 4. 추가 정보 업데이트 (선택사항)
        if (kakaoUserInfo.getGender() != null || kakaoUserInfo.getPhoneNumber() != null) {
            userService.updateUserInfo(
                user.getId(),
                null, // nickname은 이미 설정됨
                null, // profileImageUrl은 이미 설정됨
                kakaoUserInfo.getGender(),
                kakaoUserInfo.getPhoneNumber()
            );
        }
        
        log.info("카카오 로그인 처리 완료: userId={}", user.getId());
        return user;
    }
    
    /**
     * 카카오 사용자 정보 파싱
     * @param userData 카카오 API 응답 데이터
     * @return 파싱된 사용자 정보
     */
    private KakaoUserInfo parseKakaoUserInfo(Map<String, Object> userData) {
        String id = String.valueOf(userData.get("id"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) userData.get("properties");
        String nickname = properties != null ? (String) properties.get("nickname") : null;
        String profileImageUrl = properties != null ? (String) properties.get("profile_image") : null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userData.get("kakao_account");
        
        String gender = null;
        String phoneNumber = null;
        
        if (kakaoAccount != null) {
            gender = (String) kakaoAccount.get("gender");
            phoneNumber = (String) kakaoAccount.get("phone_number");
        }
        
        return new KakaoUserInfo(id, nickname, profileImageUrl, gender, phoneNumber);
    }
    
    /**
     * 카카오 사용자 정보 DTO
     */
    public static class KakaoUserInfo {
        private String id;
        private String nickname;
        private String profileImageUrl;
        private String gender;
        private String phoneNumber;
        
        public KakaoUserInfo(String id, String nickname, String profileImageUrl, 
                           String gender, String phoneNumber) {
            this.id = id;
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
            this.gender = gender;
            this.phoneNumber = phoneNumber;
        }
        
        // Getters
        public String getId() { return id; }
        public String getNickname() { return nickname; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public String getGender() { return gender; }
        public String getPhoneNumber() { return phoneNumber; }
    }
}
