package com.chimaenono.dearmind.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth Test", description = "인증 테스트 API")
public class AuthTestController {

    /**
     * 인증된 사용자 정보 조회 (JWT 토큰 필요)
     */
    @GetMapping("/profile")
    @Operation(summary = "인증된 사용자 프로필 조회", description = "JWT 토큰을 통해 인증된 사용자의 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getProfile() {
        log.info("인증된 사용자 프로필 조회 요청");
        
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증 성공");
            response.put("user", user);
            response.put("authorities", authentication.getAuthorities());
            
            log.info("인증된 사용자 프로필 조회 성공: userId={}", user.getId());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "인증되지 않은 사용자");
            
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * 인증 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "인증 상태 확인", description = "현재 인증 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        log.info("인증 상태 확인 요청");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            
            response.put("authenticated", true);
            response.put("principal", authentication.getPrincipal().getClass().getSimpleName());
            response.put("authorities", authentication.getAuthorities());
            
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                response.put("userId", user.getId());
                response.put("nickname", user.getNickname());
            }
        } else {
            response.put("authenticated", false);
            response.put("message", "인증되지 않음");
        }
        
        return ResponseEntity.ok(response);
    }
}
