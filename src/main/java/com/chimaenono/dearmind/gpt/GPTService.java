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
import com.chimaenono.dearmind.music.MusicRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
        
        // 회상 요법 기반 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[역할 설정]**\n");
        promptBuilder.append("당신은 할머니/할아버지를 사랑하는 손녀/손자입니다.\n");
        promptBuilder.append("할머니/할아버지와 함께 옛날 이야기를 나누며 즐거운 시간을 보내고 싶어합니다.\n");
        promptBuilder.append("과거의 좋은 기억들을 함께 떠올리며 따뜻한 대화를 나누는 것이 목표입니다.\n\n");
        
        promptBuilder.append("**[대화 원칙]**\n");
        promptBuilder.append("1. 할머니/할아버지의 과거 이야기를 듣고 싶어합니다\n");
        promptBuilder.append("2. 진심으로 공감하고 관심을 보입니다\n");
        promptBuilder.append("3. 구체적인 세부사항에 대해 궁금해합니다\n");
        promptBuilder.append("4. 더 많은 이야기를 듣고 싶어합니다\n");
        promptBuilder.append("5. 모든 응답은 질문으로 끝나야 합니다\n\n");
        
        promptBuilder.append("**[감정 정보]**\n");
        promptBuilder.append("사용자의 현재 감정: '").append(emotionKorean).append("' (신뢰도: ").append(confidencePercent).append("%)\n\n");
        
        promptBuilder.append("**[대화 맥락]**\n");
        if (prevUser != null && !prevUser.trim().isEmpty()) {
            promptBuilder.append("이전 할머니/할아버지: \"").append(prevUser).append("\"\n");
        }
        if (prevSys != null && !prevSys.trim().isEmpty()) {
            promptBuilder.append("이전 손녀/손자: \"").append(prevSys).append("\"\n");
        }
        promptBuilder.append("현재 할머니/할아버지: \"").append(currUser).append("\"\n\n");
        
        promptBuilder.append("**[응답 지침]**\n");
        promptBuilder.append("할머니/할아버지와의 따뜻한 대화를 위해 다음을 지켜주세요:\n\n");
        
        promptBuilder.append("1. **사랑스러운 관심**: 할머니/할아버지의 이야기에 진심으로 관심을 보여주세요\n");
        promptBuilder.append("2. **자연스러운 호기심**: 과거 이야기에 대해 자연스럽게 궁금해해주세요\n");
        promptBuilder.append("3. **구체적 질문**: '어떤', '언제', '어디서', '누구와' 같은 구체적인 질문을 해주세요\n");
        promptBuilder.append("4. **더 듣고 싶어함**: 더 많은 이야기를 듣고 싶어하는 마음을 표현해주세요\n");
        promptBuilder.append("5. **손녀/손자 톤**: 사랑하는 손녀/손자처럼 따뜻하고 애정 어린 톤으로 말해주세요\n");
        promptBuilder.append("6. **쉬운 단어**: 어려운 단어 대신 쉽고 간단한 단어를 사용해주세요\n");
        promptBuilder.append("7. **짧은 문장**: 한 문장을 짧게 나누어 말해주세요 (10-15단어 이내)\n");
        promptBuilder.append("8. **적절한 길이**: 2-3문장으로 구성하여 너무 길지 않게 해주세요\n");
        promptBuilder.append("9. **질문으로 마무리**: 반드시 응답을 질문으로 끝내주세요\n\n");
        
        promptBuilder.append("**[응답 예시 스타일]**\n");
        promptBuilder.append("- \"힘들었겠어요. 그때 비슷한 일이 있었나요?\"\n");
        promptBuilder.append("- \"그런 일이 있었군요. 그때 누구와 함께 계셨나요?\"\n");
        promptBuilder.append("- \"좋은 기억이네요. 그때 어떤 기분이셨나요?\"\n");
        promptBuilder.append("- \"정말 대단하셨어요. 그때 어디서 하셨나요?\"\n");
        promptBuilder.append("- \"와, 정말 신기해요! 그때 어떻게 하셨나요?\"\n");
        promptBuilder.append("- \"할머니/할아버지 정말 멋있으셨어요! 그때 누가 도와주셨나요?\"\n\n");
        
        promptBuilder.append("할머니/할아버지의 감정과 대화 내용을 고려하여 사랑하는 손녀/손자처럼 따뜻하고 애정 어린 응답을 생성해주세요. 반드시 질문으로 마무리해주세요.");
        
        
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
        
        // JSON 형태로 감정 분포 저장 (올바른 JSON 형식으로 변환)
        String emotionDistribution = convertMapToJson(emotionCounts);
        
        // Conversation 테이블에 저장
        conversationService.saveConversationEmotionAnalysis(conversationId, dominantEmotion, averageConfidence, emotionDistribution);
    }
    
    /**
     * 일기 내용과 감정을 바탕으로 음악을 추천합니다.
     */
    public List<MusicRecommendation> generateMusicRecommendations(
            String diary, String emotion, Double confidence) throws Exception {
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("**[역할 설정]**\n");
        promptBuilder.append("너는 사용자의 일기 내용과 감정을 분석하여, 그에 가장 잘 어울리는 음악을 추천해주는 전문적인 음악 큐레이터야.\n\n");
        
        promptBuilder.append("**[입력 데이터]**\n");
        promptBuilder.append("사용자의 일기 내용:\n");
        promptBuilder.append("\"").append(diary).append("\"\n\n");
        promptBuilder.append("사용자의 감정: '").append(emotion).append("', 신뢰도: ").append(String.format("%.0f", confidence * 100)).append("%\n\n");
        
        promptBuilder.append("**[추천 조건]**\n");
        promptBuilder.append("1. 감정: '").append(emotion).append("'에 공감하면서도 너무 우울하지 않은, 잔잔하고 따뜻한 위로를 주는 음악을 추천해줘.\n");
        promptBuilder.append("2. 장르: 인디, 발라드 장르의 곡을 선호해.\n");
        promptBuilder.append("3. 추천 개수: 3곡\n");
        promptBuilder.append("4. 반드시 실제로 존재하는 한국 가요만 추천해줘.\n");
        promptBuilder.append("5. 매우 유명하고 대중적으로 알려진 가수와 그들의 대표곡만 추천해줘.\n");
        promptBuilder.append("6. 가수와 노래 제목의 조합이 정확해야 합니다.\n");
        promptBuilder.append("7. 추천 예시: 아이유-좋은날, 이승기-삭제, 태연-만약에, 박효신-야생화\n");
        promptBuilder.append("8. 제목과 가수명을 정확하게 입력해줘.\n\n");
        
        promptBuilder.append("**[출력 형식]**\n");
        promptBuilder.append("아래 JSON 형식으로 출력해줘. YouTube 링크는 제공하지 마세요.\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"recommended_music\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"노래 제목\",\n");
        promptBuilder.append("      \"artist\": \"가수 이름\",\n");
        promptBuilder.append("      \"mood\": \"음악 분위기\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n");
        
        // GPT API 호출
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(800);
        gptRequest.setTemperature(0.7);
        gptRequest.setStream(false);
        
        GPTMessage systemMessage = new GPTMessage("system", promptBuilder.toString());
        GPTMessage userMessage = new GPTMessage("user", "음악을 추천해줘.");
        
        gptRequest.setMessages(List.of(systemMessage, userMessage));
        
        GPTResponse gptResponse = generateResponse(gptRequest);
        String responseText = gptResponse.getChoices().get(0).getMessage().getContent();
        
        System.out.println("GPT 음악 추천 응답: " + responseText);
        
        // JSON 파싱하여 MusicRecommendation 리스트로 변환
        return parseMusicRecommendations(responseText);
    }
    
    /**
     * GPT 응답을 파싱하여 MusicRecommendation 리스트로 변환합니다.
     */
    private List<MusicRecommendation> parseMusicRecommendations(String responseText) throws Exception {
        try {
            // JSON 부분만 추출 (```json ... ``` 형태일 수 있음)
            String jsonText = responseText;
            if (responseText.contains("```json")) {
                int start = responseText.indexOf("```json") + 7;
                int end = responseText.indexOf("```", start);
                if (end > start) {
                    jsonText = responseText.substring(start, end).trim();
                }
            } else if (responseText.contains("```")) {
                int start = responseText.indexOf("```") + 3;
                int end = responseText.indexOf("```", start);
                if (end > start) {
                    jsonText = responseText.substring(start, end).trim();
                }
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonText);
            JsonNode musicArray = rootNode.get("recommended_music");
            
            List<MusicRecommendation> recommendations = new ArrayList<>();
            
            if (musicArray != null && musicArray.isArray()) {
                for (JsonNode musicNode : musicArray) {
                    MusicRecommendation music = new MusicRecommendation();
                    music.setTitle(musicNode.get("title").asText());
                    music.setArtist(musicNode.get("artist").asText());
                    music.setMood(musicNode.get("mood").asText());
                    // YouTube 링크는 나중에 YouTubeSearchService에서 생성
                    music.setYoutubeLink(null);
                    recommendations.add(music);
                }
            }
            
            return recommendations;
            
        } catch (Exception e) {
            System.err.println("음악 추천 JSON 파싱 오류: " + e.getMessage());
            System.err.println("원본 응답: " + responseText);
            
            // 파싱 실패 시 기본 음악 추천 반환
            return createDefaultMusicRecommendations();
        }
    }
    
    /**
     * 파싱 실패 시 기본 음악 추천을 생성합니다.
     */
    private List<MusicRecommendation> createDefaultMusicRecommendations() {
        List<MusicRecommendation> defaultRecommendations = new ArrayList<>();
        
        // 검증된 음악 추천 1 - 아이유 좋은날
        MusicRecommendation music1 = new MusicRecommendation();
        music1.setTitle("좋은날");
        music1.setArtist("아이유");
        music1.setMood("희망적이고 밝은");
        music1.setYoutubeLink("https://www.youtube.com/watch?v=jeqdYqsrsA0");
        music1.setYoutubeVideoId("jeqdYqsrsA0");
        defaultRecommendations.add(music1);
        
        // 검증된 음악 추천 2 - 이승기 삭제
        MusicRecommendation music2 = new MusicRecommendation();
        music2.setTitle("삭제");
        music2.setArtist("이승기");
        music2.setMood("감성적이고 위로가 되는");
        music2.setYoutubeLink("https://www.youtube.com/watch?v=9bZkp7q19f0");
        music2.setYoutubeVideoId("9bZkp7q19f0");
        defaultRecommendations.add(music2);
        
        // 검증된 음악 추천 3 - 태연 만약에
        MusicRecommendation music3 = new MusicRecommendation();
        music3.setTitle("만약에");
        music3.setArtist("태연");
        music3.setMood("잔잔하고 아름다운");
        music3.setYoutubeLink("https://www.youtube.com/watch?v=0-q1KafFCLU");
        music3.setYoutubeVideoId("0-q1KafFCLU");
        defaultRecommendations.add(music3);
        
        return defaultRecommendations;
    }
    
    /**
     * Map을 올바른 JSON 형식으로 변환합니다.
     */
    private String convertMapToJson(Map<String, Integer> map) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            System.err.println("JSON 변환 오류: " + e.getMessage());
            return "{}"; // 빈 JSON 객체 반환
        }
    }
}
