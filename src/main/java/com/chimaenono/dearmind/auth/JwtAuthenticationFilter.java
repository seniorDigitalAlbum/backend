package com.chimaenono.dearmind.auth;

import com.chimaenono.dearmind.kakao.service.JwtService;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. 요청 헤더에서 "Authorization" 헤더를 찾습니다.
        String authorizationHeader = request.getHeader("Authorization");

        // 2. 헤더가 없거나 "Bearer "로 시작하지 않으면 통과시킵니다.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 부분을 제거하여 순수 토큰만 추출합니다.
        String token = authorizationHeader.substring(7);

        // 4. 토큰이 블랙리스트에 있는지 확인합니다.
        if (jwtTokenBlacklistService.isTokenBlacklisted(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 5. 토큰이 유효한지 검증합니다.
        if (jwtService.validateToken(token)) {
            // 6. 토큰에서 카카오 ID를 추출합니다.
            String kakaoId = jwtService.getKakaoIdFromToken(token);

            // 7. 카카오 ID로 DB에서 사용자를 조회합니다.
            userRepository.findByKakaoId(kakaoId).ifPresent(user -> {
                // 8. 조회된 사용자 정보로 인증 객체를 생성합니다.
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, // Principal (주요 사용자 정보)
                        null, // Credentials (자격 증명, 보통 비워둠)
                        Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())) // Authorities (권한 목록)
                );
                
                // 9. 스프링 시큐리티의 컨텍스트에 인증 정보를 설정합니다.
                // 이로써 이 요청은 인증된 사용자의 요청임을 알 수 있게 됩니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        // 10. 다음 필터로 요청을 전달합니다.
        filterChain.doFilter(request, response);
    }
}