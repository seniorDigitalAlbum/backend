package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GPTService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${openai.api.max-tokens:500}")
    private Integer defaultMaxTokens;
    
    @Value("${openai.api.temperature:0.7}")
    private Double defaultTemperature;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public GPTService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * GPT API를 호출하여 응답을 생성합니다.
     */
    public GPTResponse generateResponse(GPTRequest request) throws Exception {
        // 기본값 설정
        if (request.getModel() == null) {
            request.setModel(defaultModel);
        }
        if (request.getMax_tokens() == null) {
            request.setMax_tokens(defaultMaxTokens);
        }
        if (request.getTemperature() == null) {
            request.setTemperature(defaultTemperature);
        }
        if (request.getStream() == null) {
            request.setStream(false);
        }
        
        // JSON 변환
        String requestBody = objectMapper.writeValueAsString(request);
        
        // HTTP 요청 생성
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/chat/completions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();
        
        // API 호출
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("GPT API 호출 실패: " + response.statusCode() + " - " + response.body());
        }
        
        // 응답 파싱
        return objectMapper.readValue(response.body(), GPTResponse.class);
    }
    
    /**
     * 감정 기반 대화 응답을 생성합니다.
     */
    public String generateEmotionBasedResponse(String emotion, Double confidence, 
                                             String prevUser, String prevSys, String currUser) throws Exception {
        
        // 감정 매핑 (영어 -> 한국어)
        Map<String, String> emotionMapping = Map.of(
            "joy", "기쁨", "sadness", "슬픔", "anger", "분노",
            "fear", "불안", "surprise", "당황", "neutral", "중립",
            "hurt", "상처"
        );
        
        String emotionKorean = emotionMapping.getOrDefault(emotion, "중립");
        int confidencePercent = (int) (confidence * 100);
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[역할 설정]**\n");
        promptBuilder.append("너는 사용자의 감정에 공감하고, 그에 맞춰 대화를 이어가는 인공지능 친구야.\n\n");
        
        promptBuilder.append("**[감정 정보]**\n");
        promptBuilder.append("사용자의 현재 감정은 '").append(emotionKorean).append("'이고, 그에 대한 모델의 신뢰도는 ").append(confidencePercent).append("%야.\n\n");
        
        promptBuilder.append("**[대화 내용]**\n");
        if (prevUser != null && !prevUser.trim().isEmpty()) {
            promptBuilder.append("사용자: \"").append(prevUser).append("\"\n");
        }
        if (prevSys != null && !prevSys.trim().isEmpty()) {
            promptBuilder.append("시스템: \"").append(prevSys).append("\"\n");
        }
        promptBuilder.append("사용자: \"").append(currUser).append("\"\n\n");
        
        promptBuilder.append("**[지시]**\n");
        promptBuilder.append("사용자의 현재 감정과 대화 내용을 참고해서, 다음 시스템 응답을 생성해줘.\n");
        promptBuilder.append("- 감정에 맞는 공감적인 응답을 해줘\n");
        promptBuilder.append("- 한국어로 자연스럽게 대화해줘\n");
        promptBuilder.append("- 1-2문장으로 간결하게 답변해줘\n");
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(defaultMaxTokens);
        gptRequest.setTemperature(defaultTemperature);
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", "응답을 생성해줘.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        // GPT API 호출
        GPTResponse gptResponse = generateResponse(gptRequest);
        
        if (gptResponse.getChoices() == null || gptResponse.getChoices().isEmpty()) {
            throw new RuntimeException("GPT API 응답에 선택지가 없습니다.");
        }
        
        return gptResponse.getChoices().get(0).getMessage().getContent();
    }
}
