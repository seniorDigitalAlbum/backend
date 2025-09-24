package com.chimaenono.dearmind.kakao;

import com.chimaenono.dearmind.kakao.dto.KakaoTokenResponse;
import com.chimaenono.dearmind.kakao.dto.KakaoUserInfo;
import com.chimaenono.dearmind.kakao.dto.KakaoFriendsResponse;
import com.chimaenono.dearmind.kakao.service.KakaoAuthService;
import com.chimaenono.dearmind.kakao.service.JwtService;
import com.chimaenono.dearmind.user.UserService;
import com.chimaenono.dearmind.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
@Tag(name = "카카오 로그인", description = "카카오 로그인 관련 API")
public class KakaoLoginPageController {

    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    private final UserService userService;

    @GetMapping("/kakao")
    @Operation(summary = "카카오 로그인 페이지로 리다이렉트", description = "카카오 OAuth 로그인 페이지로 사용자를 리다이렉트합니다.")
    public ResponseEntity<Map<String, String>> kakaoLogin(
            @RequestParam(defaultValue = "web") String platform,
            @RequestParam(required = false) String ip) {
        try {
            String kakaoAuthUrl = kakaoAuthService.getKakaoAuthUrl(platform, ip);
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", kakaoAuthUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카카오 로그인 URL 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 로그인 콜백 처리", description = "카카오 OAuth 콜백을 처리하고 프론트엔드로 리다이렉트합니다.")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "ip", required = false) String ip) {
        try {
            // 1. 인증 코드로 액세스 토큰 요청
            KakaoTokenResponse tokenResponse = kakaoAuthService.getAccessToken(code);
            
            // 2. 액세스 토큰으로 사용자 정보 조회
            KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(tokenResponse.getAccessToken());
            
            // 3. 친구 목록 조회
            try {
                var friendsResponse = kakaoAuthService.getFriends(tokenResponse.getAccessToken());
                log.info("카카오 친구 목록 조회 성공: {} 명", friendsResponse.getTotalCount());
            } catch (Exception e) {
                log.warn("카카오 친구 목록 조회 실패 (앱 미가입자일 수 있음): {}", e.getMessage());
            }
            
            // 4. 기존 사용자 조회 또는 새 사용자 생성 (역할 없이)
            var existingUser = userService.getUserByKakaoId(userInfo.getKakaoId().toString());
            User user;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // 기존 사용자의 카카오 액세스 토큰 업데이트
                userService.updateKakaoAccessToken(user.getUserId(), tokenResponse.getAccessToken());
                log.info("기존 사용자 로그인: {}", user.getName());
            } else {
                // 새 사용자 생성 (역할은 null로 저장)
                user = userService.createKakaoUser(
                    userInfo.getKakaoId().toString(),
                    userInfo.getNickname(),
                    null, // 역할은 나중에 설정
                    userInfo.getGenderAsEnum(),
                    userInfo.getProfileImageUrl(),
                    tokenResponse.getAccessToken() // 카카오 액세스 토큰 추가
                );
                log.info("새 사용자 생성: {}", user.getName());
            }
            
            // 5. 사용자 정보를 기반으로 JWT 토큰 생성
            String jwtToken = jwtService.generateToken(userInfo.getKakaoId().toString(), userInfo.getNickname());
            
            // 6. 역할 선택 페이지로 직접 리다이렉트
            String redirectUrl = getUserRoleSelectionUrl(code, ip);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
            
        } catch (Exception e) {
            log.error("카카오 로그인 콜백 처리 실패", e);
            // 에러 시에도 역할 선택 페이지로 리다이렉트
            String errorUrl;
            if (ip != null && !ip.isEmpty()) {
                // 로컬 개발 환경 (IP 지정)
                errorUrl = "http://" + ip + ":8081/UserRoleSelection?error=login_failed";
            } else {
                // 프로덕션 환경 (설정된 도메인 사용)
                errorUrl = kakaoAuthService.getFrontendBaseUrl() + "/UserRoleSelection?error=login_failed";
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorUrl)
                    .build();
        }
    }

    /**
     * UserRoleSelection URL 생성 (역할 선택 페이지로 직접 리다이렉트)
     */
    private String getUserRoleSelectionUrl(String code, String ip) {
        try {
            String baseUrl;
            if (ip != null && !ip.isEmpty()) {
                // 로컬 개발 환경 (IP 지정)
                baseUrl = "http://" + ip + ":8081";
            } else {
                // 프로덕션 환경 (설정된 도메인 사용)
                baseUrl = kakaoAuthService.getFrontendBaseUrl();
            }
            
            return baseUrl + "/UserRoleSelection?code=" + code;
            
        } catch (Exception e) {
            log.error("UserRoleSelection URL 생성 실패", e);
            // 에러 시 기본 localhost 사용
            return "http://localhost:8081/UserRoleSelection?code=" + code;
        }
    }

    @PostMapping("/kakao/logout")
    @Operation(summary = "카카오 로그아웃", description = "카카오 계정에서 로그아웃합니다.")
    public ResponseEntity<Map<String, String>> kakaoLogout(@RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰에서 카카오 ID 추출
            String kakaoId = jwtService.getKakaoIdFromToken(token.replace("Bearer ", ""));
            
            // 카카오 로그아웃 처리
            kakaoAuthService.logout(kakaoId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "로그아웃이 완료되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카카오 로그아웃 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/kakao/user-info")
    @Operation(summary = "카카오 사용자 정보 조회", description = "JWT 토큰을 기반으로 현재 로그인한 사용자 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증 및 사용자 정보 추출
            String kakaoId = jwtService.getKakaoIdFromToken(token.replace("Bearer ", ""));
            String nickname = jwtService.getNicknameFromToken(token.replace("Bearer ", ""));
            
            Map<String, Object> response = new HashMap<>();
            response.put("kakaoId", kakaoId);
            response.put("nickname", nickname);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/kakao/friends")
    @Operation(summary = "카카오 친구 목록 조회", description = "현재 로그인한 사용자의 카카오 친구 목록을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getFriends(@RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰에서 카카오 ID 추출
            String kakaoId = jwtService.getKakaoIdFromToken(token.replace("Bearer ", ""));
            
            // 사용자 조회
            var userOpt = userService.getUserByKakaoId(kakaoId);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            String accessToken = user.getKakaoAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "카카오 액세스 토큰이 없습니다. 다시 로그인해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 카카오 친구 목록 조회
            var friendsResponse = kakaoAuthService.getFriends(accessToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("friends", friendsResponse);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("친구 목록 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "친구 목록 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/kakao/user-info")
    @Operation(summary = "카카오 로그인 사용자 정보 조회", description = "카카오 로그인 코드로 사용자 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUserInfoByCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "code가 필요합니다."));
            }

            // 1. 인증 코드로 액세스 토큰 요청
            KakaoTokenResponse tokenResponse = kakaoAuthService.getAccessToken(code);
            
            // 2. 액세스 토큰으로 사용자 정보 조회
            KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(tokenResponse.getAccessToken());
            
            // 3. 기존 사용자 조회
            var existingUser = userService.getUserByKakaoId(userInfo.getKakaoId().toString());
            
            // 4. JWT 토큰 생성
            String jwtToken = jwtService.generateToken(userInfo.getKakaoId().toString(), userInfo.getNickname());
            
            // 5. 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwtToken);
            response.put("kakaoUserInfo", userInfo);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                response.put("userType", user.getUserType().toString());
                response.put("isExistingUser", true);
            } else {
                response.put("isExistingUser", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 정보 조회에 실패했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/kakao/search-friends")
    @Operation(summary = "카카오 친구 중 시니어 검색", description = "카카오 친구 중 우리 앱에 가입된 시니어를 검색합니다.")
    public ResponseEntity<Map<String, Object>> searchKakaoFriends(@RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰에서 카카오 ID 추출
            String kakaoId = jwtService.getKakaoIdFromToken(token.replace("Bearer ", ""));
            
            // 사용자 조회
            var userOpt = userService.getUserByKakaoId(kakaoId);
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            String accessToken = user.getKakaoAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "카카오 액세스 토큰이 없습니다. 다시 로그인해주세요.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 카카오 친구 목록 조회
            var friendsResponse = kakaoAuthService.getFriends(accessToken);
            
            // 친구 목록에서 카카오 ID 추출
            List<Long> friendKakaoIds = friendsResponse.getElements().stream()
                .map(friend -> friend.getId())
                .collect(java.util.stream.Collectors.toList());
            
            // 우리 앱에 가입된 시니어 검색
            List<User> registeredSeniors = userService.findByKakaoIdsAndUserType(friendKakaoIds, "SENIOR");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("seniors", registeredSeniors);
            response.put("totalCount", registeredSeniors.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카카오 친구 중 시니어 검색 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "친구 검색 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/kakao/update-user-type")
    @Operation(summary = "사용자 역할 업데이트", description = "카카오 사용자의 역할을 업데이트합니다.")
    public ResponseEntity<Map<String, Object>> updateUserType(@RequestBody Map<String, String> request) {
        try {
            String kakaoId = request.get("kakaoId");
            String userType = request.get("userType");
            
            if (kakaoId == null || userType == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "kakaoId와 userType이 필요합니다."));
            }

            // 사용자 조회
            var userOpt = userService.getUserByKakaoId(kakaoId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
            }

            User user = userOpt.get();
            user.setUserType(User.UserType.valueOf(userType));
            userService.updateUser(user.getUserId(), user.getName(), user.getPhone(), user.getProfileImage());
            
            log.info("사용자 역할 업데이트 완료: {} -> {}", user.getName(), userType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "역할이 성공적으로 업데이트되었습니다.");
            response.put("user", user);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 역할 업데이트 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "역할 업데이트에 실패했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
