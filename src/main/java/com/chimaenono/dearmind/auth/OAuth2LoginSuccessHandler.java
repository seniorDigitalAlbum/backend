package com.chimaenono.dearmind.auth;

import com.chimaenono.dearmind.kakao.service.JwtService;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공!");
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            // OAuth2User로부터 사용자 정보를 받아옵니다.
            String kakaoId = oAuth2User.getAttributes().get("id").toString();
            
            // DB에서 해당 사용자를 조회합니다.
            User user = userRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            // 우리 서비스의 JWT 토큰을 생성합니다.
            String jwtToken = jwtService.generateToken(user.getKakaoId(), user.getName());
            log.info("JWT 토큰이 생성되었습니다: {}", jwtToken);

            boolean isNewUser = user.getUserType() == null;

            // 프론트엔드로 리다이렉트할 URL을 생성합니다.
            // 여기서는 토큰을 직접 쿼리 파라미터로 전달합니다.
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8081/UserRoleSelection")
                    .queryParam("token", jwtToken)
                    .queryParam("isNewUser", isNewUser)
                    .queryParam("nickname", user.getName())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            
            // 생성된 URL로 리다이렉트시킵니다.
            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("로그인 성공 후 처리 중 예외 발생", e);
            throw e;
        }
    }
}