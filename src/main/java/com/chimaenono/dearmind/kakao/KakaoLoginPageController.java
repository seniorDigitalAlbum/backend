package com.chimaenono.dearmind.kakao;

import com.chimaenono.dearmind.exception.KakaoPermissionException;
import com.chimaenono.dearmind.kakao.dto.KakaoTokenResponse;
import com.chimaenono.dearmind.kakao.dto.KakaoUserInfo;
import com.chimaenono.dearmind.kakao.service.KakaoAuthService;
import com.chimaenono.dearmind.kakao.service.JwtService;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
@Tag(name = "카카오 로그인", description = "카카오 로그인 관련 API")
public class KaKaoLoginPageController {

    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    private final UserService userService;

    @GetMapping("/kakao")
    public ResponseEntity<Map<String, String>> kakaoLogin(
            @RequestParam(defaultValue = "web") String platform,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String scope) {
        try {
            String kakaoAuthUrl = kakaoAuthService.getKakaoAuthUrl(platform, ip, scope);
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", kakaoAuthUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카카오 로그인 URL 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", defaultValue = "web") String platform) {
        try {
            KakaoTokenResponse tokenResponse = kakaoAuthService.getAccessToken(code, platform);
            KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(tokenResponse.getAccessToken());

            User user = userService.createOrUpdateKakaoUser(
                userInfo.getKakaoId().toString(),
                userInfo.getNickname(),
                null,
                userInfo.getGenderAsEnum(),
                userInfo.getProfileImageUrl(),
                tokenResponse.getAccessToken()
            );
            
            String jwtToken = jwtService.generateToken(user.getKakaoId(), user.getName());
            
            boolean isNewUser = user.getUserType() == null;
            String redirectUrl = buildFrontendRedirectUrl(jwtToken, userInfo.getNickname(), platform, isNewUser);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
            
        } catch (Exception e) {
            log.error("카카오 로그인 콜백 처리 실패", e);
            String errorUrl = getErrorRedirectUrl(platform);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorUrl)
                    .build();
        }
    }

    @PostMapping("/kakao/search-friends")
    public ResponseEntity<Map<String, Object>> searchKakaoFriends(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            String kakaoId = jwtService.getKakaoIdFromToken(jwt);
            
            var userOpt = userService.getUserByKakaoId(kakaoId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
            }
            
            User user = userOpt.get();
            String accessToken = user.getKakaoAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "카카오 액세스 토큰이 없습니다. 다시 로그인해주세요."));
            }
            
            var friendsResponse = kakaoAuthService.getFriends(accessToken);
            
            List<Long> friendKakaoIds = friendsResponse.getElements().stream()
                .map(friend -> friend.getId())
                .collect(Collectors.toList());
            
            List<User> registeredSeniors = userService.findByKakaoIdsAndUserType(friendKakaoIds, "SENIOR");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("seniors", registeredSeniors);
            response.put("totalCount", registeredSeniors.size());
            
            return ResponseEntity.ok(response);
            
        } catch (KakaoPermissionException e) {
            log.warn("친구 검색 실패: 추가 동의 필요");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage(), "error_code", "NEEDS_CONSENT"));
        } catch (Exception e) {
            log.error("카카오 친구 중 시니어 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "친구 검색 중 오류가 발생했습니다."));
        }
    }

    private String buildFrontendRedirectUrl(String token, String nickname, String platform, boolean isNewUser) {
        String encodedNickname = "";
        try {
            encodedNickname = URLEncoder.encode(nickname, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("닉네임 인코딩 실패", e);
        }

        String query = String.format("?token=%s&nickname=%s&isNewUser=%b", token, encodedNickname, isNewUser);
        String screen = isNewUser ? "UserRoleSelection" : "MainTabs";

        if ("web".equalsIgnoreCase(platform)) {
            return kakaoAuthService.getFrontendBaseUrl() + "/" + screen + query;
        } else {
            return "dearmind://" + screen + query;
        }
    }
    
    private String getErrorRedirectUrl(String platform) {
        String errorQuery = "?error=login_failed";
        String screen = "UserRoleSelection";

        if ("web".equalsIgnoreCase(platform)) {
            return kakaoAuthService.getFrontendBaseUrl() + "/" + screen + errorQuery;
        } else {
            return "dearmind://" + screen + errorQuery;
        }
    }
}