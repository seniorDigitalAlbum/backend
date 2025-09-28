package com.chimaenono.dearmind.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Auth", description = "카카오 로그인 API")
public class KakaoAuthController {
    
    private final KakaoAuthService kakaoAuthService;
    private final UserService userService;
    private final com.chimaenono.dearmind.config.JwtConfig jwtConfig;
    
    /**
     * 카카오 로그인 콜백 처리
     */
    @GetMapping("/callback")
    @Operation(summary = "카카오 로그인 콜백", description = "카카오 로그인 후 콜백을 처리하고 프론트엔드로 리다이렉트합니다.")
    public ResponseEntity<String> kakaoCallback(
            @Parameter(description = "카카오 인증 코드", required = true)
            @RequestParam("code") String code) {
        
        log.info("카카오 로그인 콜백 처리 시작: code={}", code);
        
        try {
            // 카카오 로그인 처리
            User user = kakaoAuthService.processKakaoLogin(code);
            
            // JWT 토큰 생성
            String token = jwtConfig.generateToken(user.getId(), user.getKakaoId(), user.getNickname());
            
            log.info("카카오 로그인 성공: userId={}, token 생성됨", user.getId());
            
            // HTML 페이지로 리다이렉트 (JavaScript를 사용하여 프론트엔드로 리다이렉트)
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>카카오 로그인 완료</title>
                </head>
                <body>
                    <script>
                        // 프론트엔드로 리다이렉트
                        window.location.href = 'http://localhost:8081/login?code=%s&token=%s';
                    </script>
                    <p>로그인 처리 중입니다...</p>
                </body>
                </html>
                """, code, token);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
            
        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            
            // 에러 발생 시 HTML 페이지로 리다이렉트
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>카카오 로그인 실패</title>
                </head>
                <body>
                    <script>
                        // 프론트엔드로 리다이렉트
                        window.location.href = 'http://localhost:8081/login?error=%s';
                    </script>
                    <p>로그인 처리 중 오류가 발생했습니다...</p>
                </body>
                </html>
                """, e.getMessage());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }
    
    /**
     * 카카오 로그인 URL 생성
     */
    @GetMapping("/login-url")
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 로그인을 위한 URL을 생성합니다.")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        log.info("카카오 로그인 URL 생성 요청");
        
        // 카카오 로그인 URL 구성
        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize?" +
                "client_id=a45363409e8fae86e1a436badaa55eef&" +
                "redirect_uri=http://localhost:8080/api/auth/kakao/callback&" +
                "response_type=code&" +
                "scope=name,profile_nickname,profile_image,gender,phone_number";
        
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", kakaoLoginUrl);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 카카오 로그인 테스트 (개발용)
     */
    @PostMapping("/test-login")
    @Operation(summary = "카카오 로그인 테스트", description = "개발용 카카오 로그인 테스트 API입니다.")
    public ResponseEntity<Map<String, Object>> testKakaoLogin(
            @Parameter(description = "테스트용 카카오 사용자 ID", required = true)
            @RequestParam("kakaoId") String kakaoId,
            @Parameter(description = "테스트용 닉네임", required = true)
            @RequestParam("nickname") String nickname) {
        
        log.info("카카오 로그인 테스트: kakaoId={}, nickname={}", kakaoId, nickname);
        
        try {
            // 테스트용 사용자 생성 또는 업데이트
            User user = userService.createOrUpdateUser(kakaoId, nickname, null);
            
            // JWT 토큰 생성
            String token = jwtConfig.generateToken(user.getId(), user.getKakaoId(), user.getNickname());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "테스트 로그인 성공");
            response.put("token", token);
            response.put("user", user);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카카오 로그인 테스트 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "테스트 로그인 실패: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
