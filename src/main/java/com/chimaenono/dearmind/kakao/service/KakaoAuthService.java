package com.chimaenono.dearmind.kakao.service;

import com.chimaenono.dearmind.exception.KakaoPermissionException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";

    public String getKakaoAuthUrl(String platform, String ip, String scope) {
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
                .queryParam("state", platform);
        
        String finalScope = "profile_nickname profile_image gender name";
        if (scope != null && !scope.isEmpty()) {
            finalScope += " " + scope;
        }
        builder.queryParam("scope", finalScope);

        String redirectUrl = getRedirectUrl(platform, ip);
        if (redirectUrl != null) {
            builder.queryParam("redirect_uri", redirectUrl);
        }

        return builder.build().toUriString();
    }
    
    public String getRedirectUrl(String platform, String ip) {
        switch (platform.toLowerCase()) {
            case "android":
            case "ios":
            case "mobile":
                return null;
            case "web":
            default:
                if (ip != null && !ip.isEmpty()) {
                    return "http://" + ip + ":8080/api/login/kakao/callback";
                } else {
                    return backendCallbackUrl;
                }
        }
    }
    
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
            
            String redirectUrl = getRedirectUrl(platform, null);
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

    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, request, KakaoUserInfo.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("카카오 사용자 정보 조회 실패: {}", response.getStatusCode());
                throw new RuntimeException("카카오 사용자 정보 조회 실패");
            }
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 중 오류 발생", e);
            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
        }
    }

    public KakaoFriendsResponse getFriends(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoFriendsResponse> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/api/talk/friends", HttpMethod.GET, request, KakaoFriendsResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("카카오 친구 목록 권한 부족. 추가 동의 필요: {}", e.getMessage());
            throw new KakaoPermissionException("카카오 친구 목록에 대한 추가 동의가 필요합니다.");
        } catch (Exception e) {
            log.warn("카카오 친구 목록 요청 중 오류 발생: {}", e.getMessage());
            KakaoFriendsResponse emptyResponse = new KakaoFriendsResponse();
            emptyResponse.setElements(java.util.Collections.emptyList());
            emptyResponse.setTotalCount(0);
            return emptyResponse;
        }
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void logout(String kakaoId) {
        try {
            log.info("카카오 사용자 로그아웃: {}", kakaoId);
        } catch (Exception e) {
            log.error("카카오 로그아웃 처리 중 오류 발생", e);
            throw new RuntimeException("카카오 로그아웃 처리 실패", e);
        }
    }
}