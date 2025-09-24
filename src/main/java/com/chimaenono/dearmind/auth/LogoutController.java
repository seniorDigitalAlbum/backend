// src/main/java/com/chimaenono/dearmind/auth/LogoutController.java
package com.chimaenono.dearmind.auth;

import com.chimaenono.dearmind.kakao.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Authentication API", description = "인증 관련 API")
public class LogoutController {

    private final JwtTokenBlacklistService jwtTokenBlacklistService;
    private final JwtService jwtService;

    public LogoutController(JwtTokenBlacklistService jwtTokenBlacklistService, JwtService jwtService) {
        this.jwtTokenBlacklistService = jwtTokenBlacklistService;
        this.jwtService = jwtService;
    }

    @PostMapping("/logout")
    @Operation(summary = "사용자 로그아웃", description = "Access JWT를 남은 만료시간 동안 블랙리스트에 등록합니다.")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.noContent().build(); // idempotent
        }
        String token = authorizationHeader.substring(7);
        long ttl = 0;
        try { ttl = jwtService.getRemainingSeconds(token); } catch (Exception ignored) {}
        if (ttl > 0) jwtTokenBlacklistService.blacklistToken(token, ttl);
        return ResponseEntity.noContent().build();
    }
}
