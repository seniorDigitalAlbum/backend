package com.chimaenono.dearmind.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@Service
@Tag(name = "TTS Service", description = "Text-to-Speech 변환 서비스")
public class TTSService {

    @Value("${naver.clova.client.id:}")
    private String clientId;

    @Value("${naver.clova.client.secret:}")
    private String clientSecret;

    @Value("${naver.clova.api.url:https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts}")
    private String apiUrl;

    @Operation(summary = "텍스트를 음성으로 변환", description = "Naver CLOVA Voice API를 사용하여 텍스트를 음성으로 변환합니다")
    public TTSResponse synthesizeSpeech(String text, String voice, String speed, String pitch, String volume, String format) {
        long startTime = System.currentTimeMillis();
        
        // API 키 확인
        System.out.println("Naver Clova Client ID: " + (clientId != null ? clientId.substring(0, Math.min(10, clientId.length())) + "..." : "null"));
        
        try {
            // 기본값 설정
            voice = voice != null ? voice : "nara";
            speed = speed != null ? speed : "1.0";
            pitch = pitch != null ? pitch : "0.0";
            volume = volume != null ? volume : "0.0";
            format = format != null ? format : "mp3";
            
            // HTTP 클라이언트 생성
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            // 요청 바디 생성
            String requestBody = String.format(
                "speaker=%s&volume=%s&speed=%s&pitch=%s&format=%s&text=%s",
                voice, volume, speed, pitch, format, java.net.URLEncoder.encode(text, "UTF-8")
            );
            
            // HTTP 요청 생성
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiUrl))
                    .header("X-NCP-APIGW-API-KEY-ID", clientId)
                    .header("X-NCP-APIGW-API-KEY", clientSecret)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // 요청 전송
            java.net.http.HttpResponse<byte[]> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            System.out.println("Clova TTS Response Status: " + response.statusCode());
            System.out.println("Clova TTS Response Size: " + response.body().length + " bytes");

            if (response.statusCode() == 200) {
                // 성공적인 응답
                String base64Audio = Base64.getEncoder().encodeToString(response.body());
                
                return new TTSResponse(
                    base64Audio,
                    format,
                    voice,
                    duration,
                    "success",
                    null
                );
            } else {
                // 오류 응답
                String errorMessage = new String(response.body());
                return new TTSResponse(
                    null,
                    format,
                    voice,
                    duration,
                    "error",
                    "TTS 변환 실패. Status: " + response.statusCode() + ", Response: " + errorMessage
                );
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            return new TTSResponse(
                null,
                format != null ? format : "mp3",
                voice != null ? voice : "nara",
                duration,
                "error",
                "TTS 변환 실패: " + e.getMessage()
            );
        }
    }

    @Operation(summary = "기본 TTS 변환", description = "기본 설정으로 텍스트를 음성으로 변환합니다")
    public TTSResponse synthesizeSpeech(String text) {
        return synthesizeSpeech(text, "nara", "1.0", "0.0", "0.0", "mp3");
    }
} 