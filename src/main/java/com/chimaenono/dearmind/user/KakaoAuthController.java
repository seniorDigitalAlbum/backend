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

import java.net.URLDecoder;
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
    
    @Value("${kakao.backend_callback_url:}")
    private String prodCallbackUrl;

    /**
     * [웹/모바일 공용] 카카오 로그인 URL 생성 (State 방식)
     */
    @GetMapping("/login-url")
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 로그인을 위한 URL을 생성합니다.")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl(
            @RequestParam(value = "isMobile", required = false, defaultValue = "false") boolean isMobile,
            @RequestParam(value = "frontendUrl", required = false) String frontendUrl,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer) {
        
        log.info("로그인 URL 생성 요청 받음, isMobile: {}, frontendUrl: {}, userAgent: {}, referer: {}", 
                isMobile, frontendUrl, userAgent, referer);

        // User-Agent로 모바일 감지 (fallback) - 단, 명시적으로 isMobile이 설정된 경우는 무시
        boolean detectedMobile = isMobile;
        if (userAgent != null) {
            boolean userAgentSuggestsMobile = userAgent.toLowerCase().contains("mobile") || 
                                            userAgent.toLowerCase().contains("android") || 
                                            userAgent.toLowerCase().contains("iphone");
            
            // 명시적으로 isMobile이 설정되지 않은 경우에만 User-Agent 사용
            if (isMobile == false) {
                // isMobile=false로 명시적으로 설정된 경우는 웹으로 처리
                detectedMobile = false;
            } else if (isMobile == true) {
                // isMobile=true로 명시적으로 설정된 경우는 모바일로 처리
                detectedMobile = true;
            } else {
                // isMobile이 설정되지 않은 경우에만 User-Agent 사용
                detectedMobile = userAgentSuggestsMobile;
            }
        }

        // Referer에서 도메인 추출 (배포 환경용)
        String detectedFrontendUrl = frontendUrl;
        if (frontendUrl == null && referer != null) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                detectedFrontendUrl = uri.getScheme() + "://" + uri.getHost() + 
                    (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            } catch (Exception e) {
                log.warn("Failed to parse referer URL: {}", referer, e);
            }
        }

        // state에 정보 포함
        String stateData = "isMobile=" + detectedMobile;
        if (detectedFrontendUrl != null && !detectedFrontendUrl.isEmpty()) {
            stateData += "&frontendUrl=" + URLEncoder.encode(detectedFrontendUrl, StandardCharsets.UTF_8);
        }
        String state = Base64.getUrlEncoder().encodeToString(stateData.getBytes(StandardCharsets.UTF_8));

        // 웹/모바일 구분하여 redirect_uri 설정
        String redirectUri;
        if (detectedMobile) {
            // 모바일: 항상 webRedirectUri 사용
            redirectUri = webRedirectUri;
        } else {
            // 웹: 로컬 개발에서는 localhost, 프로덕션에서는 설정된 URL 사용
            if (prodCallbackUrl != null && !prodCallbackUrl.isEmpty() && !prodCallbackUrl.contains("localhost")) {
                redirectUri = prodCallbackUrl;
            } else {
                redirectUri = "http://localhost:8080/api/auth/kakao/callback";
            }
        }
        
        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize?" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
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
        String frontendUrl = null;
        try {
            // Base64 디코딩하여 state 값들 복원
            String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            String[] stateParams = decodedState.split("&");
            
            for (String param : stateParams) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    if ("isMobile".equals(keyValue[0]) && "true".equals(keyValue[1])) {
                        isMobile = true;
                    } else if ("frontendUrl".equals(keyValue[0])) {
                        frontendUrl = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Invalid state parameter received: {}", state, e);
        }

        log.info("카카오 콜백 처리 시작: isMobile={}, frontendUrl={}", isMobile, frontendUrl);

        try {
            String finalRedirectUrl;
            if (isMobile) {
                // 모바일: JWT 토큰 생성 후 딥링크로 전달
                User user = kakaoAuthService.processKakaoLogin(code, webRedirectUri);
                String token = jwtConfig.generateToken(user.getId(), user.getKakaoId(), user.getNickname());
                finalRedirectUrl = "dearmind://kakao-auth?token=" + token;
            } else {
                // 웹: code만 전달 (토큰은 프론트엔드에서 별도 요청)
                if (frontendUrl != null && !frontendUrl.isEmpty()) {
                    // URL에 프로토콜이 없으면 https:// 추가 (배포 환경용)
                    if (!frontendUrl.startsWith("http://") && !frontendUrl.startsWith("https://")) {
                        frontendUrl = "https://" + frontendUrl;
                    }
                    finalRedirectUrl = frontendUrl + "/login?code=" + code;
                } else {
                    // 기본값은 로컬 개발용
                    finalRedirectUrl = "http://localhost:8081/login?code=" + code;
                }
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

    /**
     * [웹 전용] code로 JWT 토큰 교환
     */
    @PostMapping("/exchange-token")
    @Operation(summary = "code로 JWT 토큰 교환", description = "카카오 로그인 code를 JWT 토큰으로 교환합니다.")
    public ResponseEntity<Map<String, String>> exchangeToken(@RequestParam("code") String code) {
        try {
            // 웹용 토큰 교환: 프로덕션 콜백 URL이 있으면 사용, 없으면 로컬 개발용 localhost 사용
            String webRedirectUri = (prodCallbackUrl != null && !prodCallbackUrl.isEmpty() && !prodCallbackUrl.contains("localhost")) 
                ? prodCallbackUrl 
                : "http://localhost:8080/api/auth/kakao/callback";
            User user = kakaoAuthService.processKakaoLogin(code, webRedirectUri);
            String token = jwtConfig.generateToken(user.getId(), user.getKakaoId(), user.getNickname());
            
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId().toString());
            response.put("nickname", user.getNickname());
            response.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            response.put("profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");
            response.put("gender", user.getGender() != null ? user.getGender() : "");
            response.put("userType", user.getUserType() != null ? user.getUserType() : "");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 교환 실패", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}