package com.chimaenono.dearmind.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Base64;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.InputStream;

@Service
@Tag(name = "TTS Service", description = "Google Cloud Text-to-Speech 변환 서비스")
public class TTSService {

    @Value("${google.cloud.texttospeech.project-id:}")
    private String projectId;

    @Value("${google.cloud.texttospeech.credentials-file:}")
    private String credentialsFile;

    private static final String GOOGLE_TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken = null;
    private long tokenExpiryTime = 0;

    @Operation(summary = "텍스트를 음성으로 변환", description = "Google Cloud Text-to-Speech API를 사용하여 텍스트를 음성으로 변환합니다")
    public TTSResponse synthesizeSpeech(String text, String voice, String speed, String pitch, String volume, String format) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 기본값 설정
            voice = voice != null ? voice : "ko-KR-Neural2-A";
            speed = speed != null ? speed : "1.0";
            pitch = pitch != null ? pitch : "0.0";
            volume = volume != null ? volume : "0.0";
            format = format != null ? format : "mp3";
            
            // 액세스 토큰 가져오기
            String accessToken = getAccessToken();
            
            // 요청 바디 생성
            String requestBody = createRequestBody(text, voice, speed, pitch, volume, format);
            
            // HTTP 클라이언트 생성
            HttpClient client = HttpClient.newHttpClient();
            
            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TTS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // 요청 전송
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            System.out.println("Google TTS Response Status: " + response.statusCode());
            System.out.println("Google TTS Response: " + response.body());

            if (response.statusCode() == 200) {
                // 성공적인 응답 파싱
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                String base64Audio = jsonResponse.get("audioContent").asText();
                
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
                return new TTSResponse(
                    null,
                    format,
                    voice,
                    duration,
                    "error",
                    "TTS 변환 실패. Status: " + response.statusCode() + ", Response: " + response.body()
                );
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            System.err.println("Google Cloud TTS Error: " + e.getMessage());
            e.printStackTrace();
            
            return new TTSResponse(
                null,
                format != null ? format : "mp3",
                voice != null ? voice : "ko-KR-Neural2-A",
                duration,
                "error",
                "TTS 변환 실패: " + e.getMessage()
            );
        }
    }

    @Operation(summary = "기본 TTS 변환", description = "기본 설정으로 텍스트를 음성으로 변환합니다")
    public TTSResponse synthesizeSpeech(String text) {
        return synthesizeSpeech(text, "ko-KR-Neural2-A", "1.0", "0.0", "0.0", "mp3");
    }
    
    @Operation(summary = "TTS 요청 객체로 변환", description = "TTSRequest 객체를 사용하여 텍스트를 음성으로 변환합니다")
    public TTSResponse convertToSpeech(TTSRequest request) {
        // TTSRequest의 필드를 사용하여 기존 메서드 호출
        String voice = request.getVoiceName() != null ? request.getVoiceName() : "ko-KR-Neural2-A";
        String speed = request.getSpeed() != null ? request.getSpeed() : "1.0";
        String pitch = request.getPitch() != null ? request.getPitch() : "0.0";
        String volume = request.getVolume() != null ? request.getVolume() : "0.0";
        String format = request.getAudioEncoding() != null ? request.getAudioEncoding().toLowerCase() : "mp3";
        
        return synthesizeSpeech(request.getText(), voice, speed, pitch, volume, format);
    }
    
    /**
     * 서비스 계정을 사용하여 액세스 토큰 가져오기
     */
    private String getAccessToken() throws Exception {
        // 토큰이 아직 유효한지 확인
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return accessToken;
        }
        
        // 서비스 계정 키 파일 로드
        InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("google-cloud-credentials.json");
        if (credentialsStream == null) {
            throw new RuntimeException("서비스 계정 키 파일을 찾을 수 없습니다: " + credentialsFile);
        }
        
        // Google Credentials 생성 (Text-to-Speech API 스코프 추가)
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream)
            .createScoped("https://www.googleapis.com/auth/cloud-platform");
        
        // 액세스 토큰 요청
        credentials.refreshIfExpired();
        accessToken = credentials.getAccessToken().getTokenValue();
        
        // 토큰 만료 시간 설정 (1시간 후)
        tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000); // 55분 후 만료
        
        return accessToken;
    }
    
    /**
     * Google Cloud TTS API 요청 바디 생성
     */
    private String createRequestBody(String text, String voice, String speed, String pitch, String volume, String format) {
        try {
            // 오디오 인코딩 설정
            String audioEncoding = getAudioEncoding(format);
            
            // 요청 JSON 생성
            String requestBody = String.format(
                "{\"input\":{\"text\":\"%s\"}," +
                "\"voice\":{\"languageCode\":\"ko-KR\",\"name\":\"%s\"}," +
                "\"audioConfig\":{\"audioEncoding\":\"%s\",\"speakingRate\":%s,\"pitch\":%s,\"volumeGainDb\":%s}}",
                text.replace("\"", "\\\""),
                voice,
                audioEncoding,
                speed,
                pitch,
                volume
            );
            
            return requestBody;
        } catch (Exception e) {
            throw new RuntimeException("요청 바디 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 오디오 포맷 문자열을 Google Cloud TTS AudioEncoding으로 변환
     */
    private String getAudioEncoding(String format) {
        switch (format.toLowerCase()) {
            case "mp3":
                return "MP3";
            case "wav":
                return "LINEAR16";
            case "ogg":
                return "OGG_OPUS";
            default:
                return "MP3";
        }
    }
} 