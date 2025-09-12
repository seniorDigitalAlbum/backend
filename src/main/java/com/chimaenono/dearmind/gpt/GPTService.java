package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.chimaenono.dearmind.conversation.ConversationService;
import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
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
    
    /**
     * 대화 내용을 요약합니다.
     */
    public String generateConversationSummary(Long conversationId, Integer summaryLength) throws Exception {
        // 대화 메시지 조회
        List<ConversationMessage> messages = conversationService.getMessagesByConversationId(conversationId);
        
        if (messages.isEmpty()) {
            throw new RuntimeException("대화 메시지를 찾을 수 없습니다: " + conversationId);
        }
        
        // 대화 내용 구성
        StringBuilder conversationBuilder = new StringBuilder();
        for (ConversationMessage message : messages) {
            String sender = message.getSenderType() == ConversationMessage.SenderType.USER ? "사용자" : "시스템";
            conversationBuilder.append(sender).append(": ").append(message.getContent()).append("\n");
        }
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[지시]**\n");
        promptBuilder.append("다음은 사용자와 시스템의 대화 내용이야.\n");
        promptBuilder.append("이 대화의 핵심적인 주제와 사용자의 감정 변화에 초점을 맞춰서 ").append(summaryLength).append("자 내외로 요약해줘.\n\n");
        
        promptBuilder.append("**[대화 내용]**\n");
        promptBuilder.append(conversationBuilder.toString());
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(200); // 요약이므로 토큰 수를 줄임
        gptRequest.setTemperature(0.3); // 요약이므로 창의성보다는 정확성에 중점
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", "대화 내용을 요약해줘.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        // GPT API 호출
        GPTResponse gptResponse = generateResponse(gptRequest);
        
        if (gptResponse.getChoices() == null || gptResponse.getChoices().isEmpty()) {
            throw new RuntimeException("GPT API 응답에 선택지가 없습니다.");
        }
        
        return gptResponse.getChoices().get(0).getMessage().getContent();
    }
    
    /**
     * 대화 내용을 요약하고 데이터베이스에 저장합니다.
     */
    public String generateAndSaveConversationSummary(Long conversationId, Integer summaryLength) throws Exception {
        // 대화 메시지 조회
        List<ConversationMessage> messages = conversationService.getMessagesByConversationId(conversationId);
        
        if (messages.isEmpty()) {
            throw new RuntimeException("대화 메시지를 찾을 수 없습니다: " + conversationId);
        }
        
        // 대화 내용 구성
        StringBuilder conversationBuilder = new StringBuilder();
        for (ConversationMessage message : messages) {
            String sender = message.getSenderType() == ConversationMessage.SenderType.USER ? "사용자" : "시스템";
            conversationBuilder.append(sender).append(": ").append(message.getContent()).append("\n");
        }
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[지시]**\n");
        promptBuilder.append("다음은 사용자와 시스템의 대화 내용이야.\n");
        promptBuilder.append("이 대화의 핵심적인 주제와 사용자의 감정 변화에 초점을 맞춰서 ").append(summaryLength).append("자 내외로 요약해줘.\n\n");
        
        promptBuilder.append("**[대화 내용]**\n");
        promptBuilder.append(conversationBuilder.toString());
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(200);
        gptRequest.setTemperature(0.3);
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", "대화 내용을 요약해줘.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        // GPT API 호출
        GPTResponse gptResponse = generateResponse(gptRequest);
        
        if (gptResponse.getChoices() == null || gptResponse.getChoices().isEmpty()) {
            throw new RuntimeException("GPT API 응답에 선택지가 없습니다.");
        }
        
        String summary = gptResponse.getChoices().get(0).getMessage().getContent();
        
        // 데이터베이스에 요약 저장
        conversationService.saveConversationSummary(conversationId, summary);
        
        return summary;
    }
    
    /**
     * 요약된 대화 내용과 감정 분석 결과를 바탕으로 일기를 생성하고 저장합니다.
     */
    public String generateAndSaveDiary(Long conversationId, String summary) throws Exception {
        // 감정 분석 결과 조회
        List<UserEmotionAnalysis> emotions = userEmotionAnalysisRepository.findByConversationMessageConversationIdOrderByAnalysisTimestampAsc(conversationId);
        
        // 감정 요약 생성 및 저장
        String emotionSummary = createEmotionSummary(emotions);
        
        // 통합된 감정 분석 결과를 Conversation 테이블에 저장
        saveConversationEmotionAnalysis(conversationId, emotions);
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[역할 설정]**\n");
        promptBuilder.append("너는 사용자의 대화 내용을 바탕으로 따뜻하고 공감적인 일기를 작성하는 AI야.\n\n");
        
        promptBuilder.append("**[대화 요약]**\n");
        promptBuilder.append(summary).append("\n\n");
        
        promptBuilder.append("**[감정 분석 결과]**\n");
        promptBuilder.append(emotionSummary).append("\n\n");
        
        promptBuilder.append("**[지시]**\n");
        promptBuilder.append("위의 대화 요약과 감정 분석 결과를 바탕으로 사용자의 마음을 담은 일기를 작성해줘.\n");
        promptBuilder.append("- 사용자의 감정과 경험을 공감적으로 표현해줘\n");
        promptBuilder.append("- 따뜻하고 위로가 되는 톤으로 작성해줘\n");
        promptBuilder.append("- 200-300자 내외로 작성해줘\n");
        promptBuilder.append("- 일기 형식으로 작성해줘 (예: 오늘은...)\n");
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(400);
        gptRequest.setTemperature(0.7);
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", "일기를 작성해줘.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        // GPT API 호출
        GPTResponse gptResponse = generateResponse(gptRequest);
        
        if (gptResponse.getChoices() == null || gptResponse.getChoices().isEmpty()) {
            throw new RuntimeException("GPT API 응답에 선택지가 없습니다.");
        }
        
        String diary = gptResponse.getChoices().get(0).getMessage().getContent();
        
        // 데이터베이스에 일기 저장
        conversationService.saveConversationDiary(conversationId, diary);
        
        return diary;
    }
    
    /**
     * 감정 분석 결과를 요약합니다.
     */
    private String createEmotionSummary(List<UserEmotionAnalysis> emotions) {
        if (emotions.isEmpty()) {
            return "감정 분석 결과가 없습니다.";
        }
        
        Map<String, Integer> emotionCounts = new HashMap<>();
        double totalConfidence = 0.0;
        int analyzedCount = 0;
        
        for (UserEmotionAnalysis emotion : emotions) {
            if (emotion.getCombinedEmotion() != null && !emotion.getCombinedEmotion().isEmpty()) {
                String emotionType = emotion.getCombinedEmotion();
                emotionCounts.put(emotionType, emotionCounts.getOrDefault(emotionType, 0) + 1);
                
                if (emotion.getCombinedConfidence() != null) {
                    totalConfidence += emotion.getCombinedConfidence();
                    analyzedCount++;
                }
            }
        }
        
        // 주요 감정 찾기
        String dominantEmotion = "중립";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantEmotion = entry.getKey();
            }
        }
        
        // 평균 신뢰도 계산
        double averageConfidence = analyzedCount > 0 ? totalConfidence / analyzedCount : 0.0;
        
        return String.format("주요 감정: %s, 평균 신뢰도: %.2f, 감정 분포: %s", 
            dominantEmotion, averageConfidence, emotionCounts.toString());
    }
    
    /**
     * 통합된 감정 분석 결과를 Conversation 테이블에 저장합니다.
     */
    private void saveConversationEmotionAnalysis(Long conversationId, List<UserEmotionAnalysis> emotions) {
        if (emotions.isEmpty()) {
            conversationService.saveConversationEmotionAnalysis(conversationId, "중립", 0.0, "{}");
            return;
        }
        
        Map<String, Integer> emotionCounts = new HashMap<>();
        double totalConfidence = 0.0;
        int analyzedCount = 0;
        
        for (UserEmotionAnalysis emotion : emotions) {
            if (emotion.getCombinedEmotion() != null && !emotion.getCombinedEmotion().isEmpty()) {
                String emotionType = emotion.getCombinedEmotion();
                emotionCounts.put(emotionType, emotionCounts.getOrDefault(emotionType, 0) + 1);
                
                if (emotion.getCombinedConfidence() != null) {
                    totalConfidence += emotion.getCombinedConfidence();
                    analyzedCount++;
                }
            }
        }
        
        // 주요 감정 찾기
        String dominantEmotion = "중립";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantEmotion = entry.getKey();
            }
        }
        
        // 평균 신뢰도 계산
        double averageConfidence = analyzedCount > 0 ? totalConfidence / analyzedCount : 0.0;
        
        // JSON 형태로 감정 분포 저장
        String emotionDistribution = emotionCounts.toString();
        
        // Conversation 테이블에 저장
        conversationService.saveConversationEmotionAnalysis(conversationId, dominantEmotion, averageConfidence, emotionDistribution);
    }
}
