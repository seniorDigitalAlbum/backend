package com.chimaenono.dearmind.config;

import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // JWT 토큰 검증
                if (jwtConfig.validateToken(token)) {
                    // 토큰에서 사용자 ID 추출
                    Long userId = jwtConfig.getUserIdFromToken(token);
                    
                    if (userId != null) {
                        // 사용자 정보 조회
                        Optional<User> userOpt = userService.findById(userId);
                        
                        if (userOpt.isPresent() && userOpt.get().getIsActive()) {
                            User user = userOpt.get();
                            
                            // Spring Security 인증 객체 생성
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                            
                            // SecurityContext에 인증 정보 설정
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("JWT 인증 성공: userId={}, nickname={}", user.getId(), user.getNickname());
                        } else {
                            log.warn("비활성화된 사용자 또는 존재하지 않는 사용자: userId={}", userId);
                        }
                    }
                } else {
                    log.warn("유효하지 않은 JWT 토큰");
                }
            } catch (Exception e) {
                log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            }
        }
        
        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
