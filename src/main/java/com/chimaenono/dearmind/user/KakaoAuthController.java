package com.chimaenono.dearmind.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Auth", description = "카카오 로그인 API")
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;
    private final com.chimaenono.dearmind.config.JwtConfig jwtConfig;

    @Value("${security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.registration.kakao.redirect-uri}")
    private String webRedirectUri;

    /**
     * [웹/모바일 공용] 카카오 로그인 URL 생성 (State 방식)
     */
    @GetMapping("/login-url")
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 로그인을 위한 URL을 생성합니다.")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl(
            @RequestParam(value = "isMobile", required = false, defaultValue = "false") boolean isMobile) {
        
        log.info("로그인 URL 생성 요청 받음, isMobile: {}", isMobile);

        // 'isMobile' 상태를 Base64로 인코딩하여 state 파라미터로 사용
        String state = Base64.getUrlEncoder().encodeToString(("isMobile=" + isMobile).getBytes(StandardCharsets.UTF_8));

        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize?" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + webRedirectUri + "&" +
                "response_type=code&" +
                "state=" + state + "&" + // state 파라미터 추가
                "scope=name,profile_nickname,profile_image,gender,phone_number";

        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", kakaoLoginUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * [웹/모바일 공용] 카카오 로그인 콜백 처리 (State 방식)
     */
    @GetMapping("/callback")
    @Operation(summary = "카카오 로그인 콜백", description = "카카오 로그인 후 콜백을 처리하고 프론트엔드로 리다이렉트합니다.")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) { // state 파라미터 받기

        boolean isMobile = false;
        try {
            // Base64 디코딩하여 isMobile 값 복원
            String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            if ("isMobile=true".equals(decodedState)) {
                isMobile = true;
            }
        } catch (Exception e) {
            log.warn("Invalid state parameter received: {}", state, e);
        }

        log.info("카카오 콜백 처리 시작: isMobile={}", isMobile);

        try {
            User user = kakaoAuthService.processKakaoLogin(code, webRedirectUri);
            String token = jwtConfig.generateToken(user.getId(), user.getKakaoId(), user.getNickname());
            
            String finalRedirectUrl;
            if (isMobile) {
                // 모바일이면 dearmind:// 딥링크로 리다이렉트
                finalRedirectUrl = "dearmind://kakao-auth?token=" + token;
            } else {
                // 웹이면 웹 프론트엔드 주소로 리다이렉트
                finalRedirectUrl = "http://localhost:8081/login?token=" + token;
            }
            
            log.info("최종 목적지로 리다이렉트: {}", finalRedirectUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", finalRedirectUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 리다이렉트

        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            String errorMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            String errorRedirectUrl = isMobile
                ? "dearmind://kakao-auth?error=" + errorMessage
                : "http://localhost:8081/login?error=" + errorMessage;
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", errorRedirectUrl);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}