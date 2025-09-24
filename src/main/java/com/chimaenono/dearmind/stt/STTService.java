package com.chimaenono.dearmind.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Base64;
import java.util.Map;

@Service
@Tag(name = "STT Service", description = "Speech-to-Text 변환 서비스")
public class STTService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/audio/transcriptions}")
    private String openaiApiUrl;

    @Operation(summary = "오디오를 텍스트로 변환", description = "Whisper API를 사용하여 오디오를 텍스트로 변환합니다")
    public STTResponse transcribeAudio(String audioData, String format, String language) {
        long startTime = System.currentTimeMillis();
        
        // API 키 확인
        System.out.println("OpenAI API Key: " + (openaiApiKey != null ? openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())) + "..." : "null"));
        
        try {
            // Base64 디코딩
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            
            // 간단한 HTTP 클라이언트로 변경
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            // 멀티파트 바디 생성
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintWriter writer = new java.io.PrintWriter(baos);
            
            // 파일 파트
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.").append(format).append("\"\r\n");
            writer.append("Content-Type: audio/").append(format).append("\r\n\r\n");
            writer.flush();
            baos.write(audioBytes);
            writer.append("\r\n");
            
            // 모델 파트
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            writer.append("whisper-1\r\n");
            
            // 바운더리 종료
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.openai.com/v1/audio/transcriptions"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            System.out.println("Whisper API Response Status: " + response.statusCode());
            System.out.println("Whisper API Response Body: " + response.body());

            if (response.statusCode() == 200) {
                // JSON 파싱
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
                
                if (responseMap.containsKey("text")) {
                    String transcribedText = (String) responseMap.get("text");
                    
                    // 텍스트 검증
                    if (!isValidKoreanText(transcribedText)) {
                        return new STTResponse(
                            null,
                            language != null ? language : "unknown",
                            0.0,
                            duration,
                            "error",
                            "음성을 인식할 수 없습니다. 다시 말씀해주세요."
                        );
                    }
                    
                    return new STTResponse(
                        transcribedText,
                        language != null ? language : "unknown",
                        0.95,
                        duration,
                        "success",
                        null
                    );
                }
            } else if (response.statusCode() == 429) {
                return new STTResponse(
                    null,
                    language != null ? language : "unknown",
                    0.0,
                    duration,
                    "error",
                    "API 요청 한도 초과. 잠시 후 다시 시도해주세요. (Status: 429)"
                );
            }
            
            return new STTResponse(
                null,
                language != null ? language : "unknown",
                0.0,
                duration,
                "error",
                "변환 결과를 찾을 수 없습니다. Status: " + response.statusCode() + ", Response: " + response.body()
            );

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            return new STTResponse(
                null,
                language != null ? language : "unknown",
                0.0,
                duration,
                "error",
                "STT 변환 실패: " + e.getMessage()
            );
        }
    }

    @Operation(summary = "실시간 STT 테스트", description = "실시간 오디오 스트림을 텍스트로 변환합니다")
    public STTResponse transcribeRealtime(String audioData) {
        return transcribeAudio(audioData, "wav", "ko");
    }

    /**
     * 한국어 텍스트 유효성 검증
     * @param text 검증할 텍스트
     * @return 유효한 한국어 텍스트인지 여부
     */
    private boolean isValidKoreanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 한국어 문자, 숫자, 공백, 기본 구두점 허용 (일본어, 중국어 제외)
        return text.matches("^[가-힣0-9\\s.,!?]*$");
    }

} 