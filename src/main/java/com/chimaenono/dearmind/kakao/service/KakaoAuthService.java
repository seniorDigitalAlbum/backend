package com.chimaenono.dearmind.kakao.service;

import com.chimaenono.dearmind.kakao.dto.KakaoTokenResponse;
import com.chimaenono.dearmind.kakao.dto.KakaoUserInfo;
import com.chimaenono.dearmind.kakao.dto.KakaoFriendsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final RestTemplate restTemplate;

    @Value("${kakao.web_client_id}")
    private String webClientId;
    
    @Value("${kakao.android_client_id}")
    private String androidClientId;
    
    @Value("${kakao.ios_client_id}")
    private String iosClientId;
    
    @Value("${kakao.rest_api_key}")
    private String restApiKey;
    
    @Value("${kakao.frontend_base_url:http://localhost:8081}")
    private String frontendBaseUrl;
    
    @Value("${kakao.backend_callback_url:http://localhost:8080/api/login/kakao/callback}")
    private String backendCallbackUrl;

    // redirect_url은 플랫폼별로 동적으로 생성됨

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

    /**
     * 카카오 OAuth 인증 URL 생성
     */
    public String getKakaoAuthUrl() {
        return getKakaoAuthUrl("web"); // 기본값은 웹
    }
    
    /**
     * 플랫폼별 리다이렉트 URL 생성
     */
    public String getRedirectUrl(String platform) {
        return getRedirectUrl(platform, null);
    }
    
    /**
     * 플랫폼별 리다이렉트 URL 생성 (IP 지정 가능)
     */
    public String getRedirectUrl(String platform, String ip) {
        switch (platform.toLowerCase()) {
            case "android":
            case "ios":
            case "mobile":
                // 모바일은 리다이렉트 URI 없음 (카카오 앱에서 직접 처리)
                return null;
            case "web":
            default:
                // 모든 플랫폼에서 백엔드 콜백 사용
                if (ip != null && !ip.isEmpty()) {
                    // 로컬 개발 환경 (IP 지정)
                    return "http://" + ip + ":8080/api/login/kakao/callback";
                } else {
                    // 프로덕션 환경 (설정값 사용)
                    return backendCallbackUrl;
                }
        }
    }
    
    /**
     * 플랫폼별 카카오 OAuth 인증 URL 생성
     */
    public String getKakaoAuthUrl(String platform) {
        return getKakaoAuthUrl(platform, null);
    }
    
    /**
     * 플랫폼별 카카오 OAuth 인증 URL 생성 (IP 지정 가능)
     */
    public String getKakaoAuthUrl(String platform, String ip) {
        String clientId;
        switch (platform.toLowerCase()) {
            case "android":
                clientId = androidClientId;
                break;
            case "ios":
                clientId = iosClientId;
                break;
            case "web":
            default:
                clientId = webClientId;
                break;
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(KAKAO_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "profile_nickname", "profile_image", "friends", "gender");
        
        // 웹 환경에서만 리다이렉트 URI 추가
        String redirectUrl = getRedirectUrl(platform, ip);
        if (redirectUrl != null) {
            builder.queryParam("redirect_uri", redirectUrl);
        }
        
        return builder.build().toUriString();
    }

    /**
     * 인증 코드로 액세스 토큰 요청
     */
    public KakaoTokenResponse getAccessToken(String code) {
        return getAccessToken(code, "web"); // 기본값은 웹
    }
    
    /**
     * 플랫폼별 인증 코드로 액세스 토큰 요청
     */
    public KakaoTokenResponse getAccessToken(String code, String platform) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String clientId;
            switch (platform.toLowerCase()) {
                case "android":
                    clientId = androidClientId;
                    break;
                case "ios":
                    clientId = iosClientId;
                    break;
                case "web":
                default:
                    clientId = webClientId;
                    break;
            }

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("grant_type", "authorization_code");
                    params.add("client_id", clientId);
                    
                    // 웹 환경에서만 redirect_uri 추가
                    String redirectUrl = getRedirectUrl(platform);
                    if (redirectUrl != null) {
                        params.add("redirect_uri", redirectUrl);
                    }
                    
                    params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    KAKAO_TOKEN_URL, request, KakaoTokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("카카오 액세스 토큰 획득 성공");
                return response.getBody();
            } else {
                log.error("카카오 액세스 토큰 획득 실패: {}", response.getStatusCode());
                throw new RuntimeException("카카오 액세스 토큰 획득 실패");
            }
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 요청 중 오류 발생", e);
            throw new RuntimeException("카카오 액세스 토큰 요청 실패", e);
        }
    }

    /**
     * 액세스 토큰으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, request, KakaoUserInfo.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                KakaoUserInfo userInfo = response.getBody();
                log.info("카카오 사용자 정보 조회 성공: {}", userInfo.getKakaoId());
                log.info("닉네임: {}", userInfo.getNickname());
                log.info("성별: {}", userInfo.getGender());
                return userInfo;
            } else {
                log.error("카카오 사용자 정보 조회 실패: {}", response.getStatusCode());
                throw new RuntimeException("카카오 사용자 정보 조회 실패");
            }
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 중 오류 발생", e);
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    /**
     * 카카오 친구 목록 조회 (앱 가입자만)
     * 이용 중 동의로 설정되어 있어서 권한이 없을 수 있음
     */
    public KakaoFriendsResponse getFriends(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoFriendsResponse> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/api/talk/friends", HttpMethod.GET, request, KakaoFriendsResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("카카오 친구 목록 조회 성공: {} 명", response.getBody().getTotalCount());
                return response.getBody();
            } else {
                log.warn("카카오 친구 목록 조회 실패: {} - 권한이 없거나 이용 중 동의가 필요할 수 있습니다", response.getStatusCode());
                // 권한이 없을 때 빈 결과 반환
                KakaoFriendsResponse emptyResponse = new KakaoFriendsResponse();
                emptyResponse.setElements(java.util.Collections.emptyList());
                emptyResponse.setTotalCount(0);
                return emptyResponse;
            }
        } catch (Exception e) {
            log.warn("카카오 친구 목록 요청 중 오류 발생 - 권한이 없을 수 있습니다: {}", e.getMessage());
            // 권한이 없을 때 빈 결과 반환
            KakaoFriendsResponse emptyResponse = new KakaoFriendsResponse();
            emptyResponse.setElements(java.util.Collections.emptyList());
            emptyResponse.setTotalCount(0);
            return emptyResponse;
        }
    }

    /**
     * 프론트엔드 베이스 URL 조회
     */
    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    /**
     * 카카오 로그아웃
     */
    public void logout(String kakaoId) {
        try {
            // 실제 구현에서는 액세스 토큰이 필요하지만, 
            // 여기서는 간단히 로그만 남김
            log.info("카카오 사용자 로그아웃: {}", kakaoId);
            
            // 필요시 실제 카카오 로그아웃 API 호출
            // HttpHeaders headers = new HttpHeaders();
            // headers.set("Authorization", "Bearer " + accessToken);
            // HttpEntity<String> request = new HttpEntity<>(headers);
            // restTemplate.postForEntity(KAKAO_LOGOUT_URL, request, String.class);
            
        } catch (Exception e) {
            log.error("카카오 로그아웃 처리 중 오류 발생", e);
            throw new RuntimeException("카카오 로그아웃 처리 실패", e);
        }
    }
}
