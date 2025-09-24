package com.chimaenono.dearmind.kakao.service;

import com.chimaenono.dearmind.exception.KakaoPermissionException;
import com.chimaenono.dearmind.kakao.dto.KakaoFriendsResponse;
import com.chimaenono.dearmind.kakao.dto.KakaoTokenResponse;
import com.chimaenono.dearmind.kakao.dto.KakaoUserInfo;
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

    @Value("${kakao.frontend_base_url:http://localhost:8081}")
    private String frontendBaseUrl;

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public KakaoFriendsResponse getFriends(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoFriendsResponse> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/api/talk/friends", HttpMethod.GET, request, KakaoFriendsResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                // 이 경우는 거의 발생하지 않음. 실패 시 예외가 발생하기 때문.
                throw new RuntimeException("카카오 친구 목록 조회에 실패했습니다.");
            }
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("카카오 친구 목록 권한 부족. 추가 동의 필요: {}", e.getMessage());
            throw new KakaoPermissionException("카카오 친구 목록에 대한 추가 동의가 필요합니다.");
        } catch (Exception e) {
            log.error("카카오 친구 목록 요청 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("카카오 친구 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}