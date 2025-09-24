package com.chimaenono.dearmind.kakao.service;



import io.jsonwebtoken.*;

import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;



import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.util.Date;



@Slf4j

@Service

public class JwtService {



    @Value("${jwt.secret:dearmind-jwt-secret-key-2024}")

    private String secretKey;



    @Value("${jwt.expiration:86400000}") // 24시간

    private long expirationTime;



    /**

     * JWT 토큰 생성

     */

    public String generateToken(String kakaoId, String nickname) {

        try {

            Date now = new Date();

            Date expiryDate = new Date(now.getTime() + expirationTime);



            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));



            return Jwts.builder()

                    .setSubject(kakaoId)

                    .claim("kakaoId", kakaoId)

                    .claim("nickname", nickname)

                    .setIssuedAt(now)

                    .setExpiration(expiryDate)

                    .signWith(key, SignatureAlgorithm.HS256)

                    .compact();

        } catch (Exception e) {

            log.error("JWT 토큰 생성 실패", e);

            throw new RuntimeException("JWT 토큰 생성 실패", e);

        }

    }



    /**

     * JWT 토큰에서 카카오 ID 추출

     */

    public String getKakaoIdFromToken(String token) {

        try {

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            

            Claims claims = Jwts.parserBuilder()

                    .setSigningKey(key)

                    .build()

                    .parseClaimsJws(token)

                    .getBody();



            return claims.get("kakaoId", String.class);

        } catch (Exception e) {

            log.error("JWT 토큰에서 카카오 ID 추출 실패", e);

            throw new RuntimeException("유효하지 않은 토큰", e);

        }

    }



    /**

     * JWT 토큰에서 닉네임 추출

     */

    public String getNicknameFromToken(String token) {

        try {

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            

            Claims claims = Jwts.parserBuilder()

                    .setSigningKey(key)

                    .build()

                    .parseClaimsJws(token)

                    .getBody();



            return claims.get("nickname", String.class);

        } catch (Exception e) {

            log.error("JWT 토큰에서 닉네임 추출 실패", e);

            throw new RuntimeException("유효하지 않은 토큰", e);

        }

    }



    /**

     * JWT 토큰 유효성 검증

     */

    public boolean validateToken(String token) {

        try {

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            

            Jwts.parserBuilder()

                    .setSigningKey(key)

                    .build()

                    .parseClaimsJws(token);

            

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            log.error("JWT 토큰 검증 실패", e);

            return false;

        }

    }



    /**

     * JWT 토큰에서 만료 시간 추출

     */

    public Date getExpirationDateFromToken(String token) {

        try {

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            

            Claims claims = Jwts.parserBuilder()

                    .setSigningKey(key)

                    .build()

                    .parseClaimsJws(token)

                    .getBody();



            return claims.getExpiration();

        } catch (Exception e) {

            log.error("JWT 토큰에서 만료 시간 추출 실패", e);

            throw new RuntimeException("유효하지 않은 토큰", e);

        }

    }



    /**

     * JWT 토큰 만료 여부 확인

     */

    public boolean isTokenExpired(String token) {

        try {

            Date expirationDate = getExpirationDateFromToken(token);

            return expirationDate.before(new Date());

        } catch (Exception e) {

            return true;

        }

    }

}

