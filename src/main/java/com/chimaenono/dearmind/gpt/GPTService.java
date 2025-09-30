package com.chimaenono.dearmind.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.chimaenono.dearmind.conversation.ConversationService;
import com.chimaenono.dearmind.conversation.EmotionFlowService;
import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;
import com.chimaenono.dearmind.music.MusicRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import lombok.extern.slf4j.Slf4j;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Slf4j
@Service
public class GPTService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    @Value("${openai.api.model:gpt-4o-mini}")
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
    private EmotionFlowService emotionFlowService;
    
    @Autowired
    private com.chimaenono.dearmind.diary.DiaryPlanService diaryPlanService;
    
    
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
    
    @Autowired
    @Lazy
    private GPTServiceNew gptServiceNew;
    
    /**
     * 감정 기반 대화 응답을 생성합니다 (새로운 프롬프트 구조).
     * GPTServiceNew에 위임합니다.
     * @return JSON 형태의 응답 (Map<String, Object>)
     */
    public Map<String, Object> generateEmotionBasedResponse(
            String emotion, 
            Double confidence, 
            String prevUser, 
            String prevSys, 
            String currUser,
            String topicRoot,
            int stepIndex,
            int ruleStep,
            List<String> facetHistory,
            Map<String, String> targetAnchor) throws Exception {
        
        // GPTServiceNew에 위임
        return gptServiceNew.generateEmotionBasedResponse(
            emotion, confidence, prevUser, prevSys, currUser,
            topicRoot, stepIndex, ruleStep, facetHistory, targetAnchor
        );
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
        promptBuilder.append("당신은 회상요법을 돕는 '대화 요약가'입니다.\n");
        promptBuilder.append("목표: 사용자와 AI의 대화 내용을 사실 그대로, 짧고 쉬운 한국어로 요약합니다.\n");
        promptBuilder.append("금지: 새로운 사실 창작, 평가/훈계, 의학적 조언, 감정 해석·추측, 과장 표현.\n");
        promptBuilder.append("출력은 지정한 JSON 스키마만 반환합니다(추가 텍스트 금지).\n\n");
        
        promptBuilder.append("다음 대화 기록을 요약하세요.\n\n");
        
        promptBuilder.append("[요약 규칙]\n");
        promptBuilder.append("- 길이: 250~350자(한국어), 문장 3~4개.\n");
        promptBuilder.append("- 시간순 서술(처음→중간→끝).\n");
        promptBuilder.append("- 사용자가 직접 말한 핵심 사건/생각 3~5개를 선택.\n");
        promptBuilder.append("- 회상 단서(사람/장소/시대/물건)를 추출.\n");
        promptBuilder.append("- 대표 인용문은 사용자 발화 중 1문장 이내 1개(없으면 빈 배열).\n");
        promptBuilder.append("- 감정 흐름이나 해석은 쓰지 말 것(패턴, 분위기, 긍/부정 등 금지).\n\n");
        
        promptBuilder.append("[입력]\n");
        promptBuilder.append("- 대화 기록(최신이 맨 아래):\n");
        promptBuilder.append(conversationBuilder.toString());
        promptBuilder.append("\n");
        
        promptBuilder.append("[출력 스키마(JSON만)]\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"situation\": \"대화 주제를 1~2문장으로 사실 위주 요약\",\n");
        promptBuilder.append("  \"events\": [\"핵심 사건/생각 3~5개(사실만)\"],\n");
        promptBuilder.append("  \"anchors\": {\n");
        promptBuilder.append("    \"people\": [\"사람(치환어)\"],\n");
        promptBuilder.append("    \"place\": [\"장소(치환어)\"],\n");
        promptBuilder.append("    \"era\": \"연대/시기(가능하면)\",\n");
        promptBuilder.append("    \"objects\": [\"물건/음식/노래 등(선택)\"]\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  \"highlights\": {\n");
        promptBuilder.append("    \"best_moment\": \"좋았던 순간(사실 한 줄, 없으면 빈 문자열)\",\n");
        promptBuilder.append("    \"hard_moment\": \"어려웠던 순간(사실 한 줄, 없으면 빈 문자열)\",\n");
        promptBuilder.append("    \"insight\": \"사용자가 말한 깨달음/생각(없으면 빈 문자열)\"\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  \"quotes\": [\"사용자 원문 1문장(선택, 최대 1개)\"]\n");
        promptBuilder.append("}");
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(800); // JSON 응답을 위해 토큰 수 증가
        gptRequest.setTemperature(0.1); // 정확한 JSON 형식을 위해 낮은 온도
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage userMessage = new GPTMessage("user", promptBuilder.toString());
        
        gptRequest.setMessages(List.of(userMessage));
        
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
        // 기본 요약 생성 함수 호출
        String summary = generateConversationSummary(conversationId, summaryLength);
        
        // 데이터베이스에 요약 저장
        conversationService.saveConversationSummary(conversationId, summary);
        
        return summary;
    }
    
    /**
     * 요약된 대화 내용과 감정 분석 결과를 바탕으로 일기를 생성하고 저장합니다.
     */
    public String generateAndSaveDiary(Long conversationId, String summary) throws Exception {
        // DiaryPlan 생성
        com.chimaenono.dearmind.diary.DiaryPlan diaryPlan = diaryPlanService.buildDiaryPlan(conversationId);
        log.info("DiaryPlan 생성 완료: conversationId={}, segments={}, pattern={}", 
                conversationId, diaryPlan.getSegments().size(), diaryPlan.getFlowPattern());
        
        // 감정 흐름 분석 결과를 Conversation 테이블에 저장
        emotionFlowService.computeAndSaveFlow(conversationId);
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 회상요법을 돕는 공감형 '일기 작가'입니다.\n");
        promptBuilder.append("목표: 제공된 요약(JSON)과 DiaryPlan만을 근거로, 한국어 1인칭 과거형 일기를 작성합니다.\n");
        promptBuilder.append("규칙:\n");
        promptBuilder.append("- 사실 준수: 요약에 없는 새로운 사실을 만들지 마세요.\n");
        promptBuilder.append("- 문체: 쉬운 단어, 짧은 문장(한 문장 20~30단어), 과장·훈계 금지.\n");
        promptBuilder.append("- 구조: 3단락(각 5~6문장). 시작-전환-마무리의 정서 흐름을 DiaryPlan에 맞게 구성.\n");
        promptBuilder.append("- 정서: DiaryPlan.styleHints(toneStart/mid/end)에 맞는 어조 사용.\n\n");
        
        promptBuilder.append("출력 형식:\n");
        promptBuilder.append("- 제목 1줄 + 본문. JSON 금지, 추가 설명 금지.\n\n");
        
        promptBuilder.append("다음 입력으로 일기를 작성하세요.\n\n");
        
        promptBuilder.append("[요약(JSON)]\n");
        promptBuilder.append(summary).append("\n");
        promptBuilder.append("// 스키마 예시:\n");
        promptBuilder.append("// {\"meta\":{\"date\":\"2025-09-16\",\"locale\":\"ko-KR\"},\n");
        promptBuilder.append("//  \"situation\":\"...\",\n");
        promptBuilder.append("//  \"events\":[\"...\",\"...\",\"...\"],\n");
        promptBuilder.append("//  \"anchors\":{\"people\":[\"가족\"],\"place\":[\"동네 골목\"],\"era\":\"초등학교 시절\",\"objects\":[\"라디오\"]},\n");
        promptBuilder.append("//  \"highlights\":{\"best_moment\":\"...\",\"hard_moment\":\"...\",\"insight\":\"...\"},\n");
        promptBuilder.append("//  \"quotes\":[\"사용자 원문 1문장\"]}\n\n");
        
        promptBuilder.append("[DiaryPlan(JSON)]\n");
        // TODO: DiaryPlan JSON을 여기에 추가 (현재는 임시로 빈 값)
        promptBuilder.append("{\"flowPattern\":\"안정형\",\"opening\":{\"dominant\":\"중립\"},\"closing\":{\"dominant\":\"중립\"},\"styleHints\":{\"toneStart\":\"차분한\",\"toneMid\":\"편안한\",\"toneEnd\":\"따뜻한\"}}\n");
        promptBuilder.append("// 예시 키: \n");
        promptBuilder.append("// {\"flowPattern\":\"U-shape\",\n");
        promptBuilder.append("//  \"opening\":{\"dominant\":\"슬픔\",\"valenceMean\":-0.8},\n");
        promptBuilder.append("//  \"turningPoint\":{\"turn\":6,\"dominant\":\"불안\"},\n");
        promptBuilder.append("//  \"closing\":{\"dominant\":\"기쁨\",\"valenceMean\":+0.7},\n");
        promptBuilder.append("//  \"styleHints\":{\"toneStart\":\"차분·다독임\",\"toneMid\":\"긴장 완화\",\"toneEnd\":\"안도·감사\"}}\n\n");
        
        promptBuilder.append("[작성 지침]\n");
        promptBuilder.append("1) 1단락(시작): summary.situation과 DiaryPlan.opening에 맞춰 사실을 짧게 회고하고 toneStart로 말하세요.\n");
        promptBuilder.append("2) 2단락(전환): turningPoint 근처 사건/생각을 events·anchors에서 골라 연결하세요. toneMid로 부드럽게 전환을 표현하세요.\n");
        promptBuilder.append("3) 3단락(마무리): closing에 맞춰 오늘의 깨달음/감사(= summary.highlights.insight/best_moment 활용)를 한두 문장으로 정리하고 toneEnd로 마치세요.\n\n");
        
        promptBuilder.append("추가 규칙:\n");
        promptBuilder.append("- quotes가 있으면 한 문장만 자연스럽게 녹여 쓰되 따옴표는 생략해도 됩니다.\n");
        promptBuilder.append("- 시대/장소/사람/물건(anchors)은 1~3개만 가볍게 언급.\n");
        promptBuilder.append("- 감정 용어를 나열하지 말고, 어조로만 드러내세요.\n");
        
        // GPT 요청 생성
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(600); // 3단락 구조의 일기를 위해 토큰 수 증가
        gptRequest.setTemperature(0.4); // 사실 기반 작성을 위해 창의성 낮춤
        gptRequest.setStream(false);
        
        // 메시지 구성
        GPTMessage userMessage = new GPTMessage("user", promptBuilder.toString());
        
        gptRequest.setMessages(List.of(userMessage));
        
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
     * DiaryPlan과 Summary를 바탕으로 시니어 친화적인 음악을 추천합니다.
     */
    public List<MusicRecommendation> generateMusicRecommendations(
            com.chimaenono.dearmind.diary.DiaryPlan diaryPlan, 
            com.chimaenono.dearmind.diary.Summary summary) throws Exception {
        
        // 프롬프트 구성
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 60–70대 사용자를 위한 '음악 큐레이터'입니다.\n");
        promptBuilder.append("목표: 주어진 DiaryPlan(정서 아크)과 Summary(내용/시대 단서)만을 근거로, 일기를 읽는 동안 듣기 좋은 음악 1개를 추천합니다.\n\n");
        
        promptBuilder.append("규칙:\n");
        promptBuilder.append("- 감정 매칭: DiaryPlan.flowPattern과 styleHints(toneStart/mid/end), closing.dominant·valenceMean을 우선 반영.\n");
        promptBuilder.append("- 시대 매칭: \n");
        promptBuilder.append("  1970~1990년대 국내 가요/포크/발라드/시티팝 또는 동시대 올드팝을 기본 가정.\n");
        promptBuilder.append("- 청감 안전(시니어 친화): 과도한 고음/소음/급격한 드롭·크레센도·BPM>120 회피. 중간 이하 볼륨, 단순 리듬, 부드러운 음색(피아노·기타·현·재즈 트리오) 우선.\n");
        promptBuilder.append("- 가사 방해 최소화: 하강/슬픔/상처·불안 톤 구간은 연주곡 우선 권장.\n");
        promptBuilder.append("- 환각 금지: 존재하지 않는 곡/아티스트명을 만들지 마세요.\n");
        promptBuilder.append("- 출력은 지정 JSON 스키마만. 링크/설명문 금지.\n\n");
        
        promptBuilder.append("감정→음악 매핑 가이드:\n");
        promptBuilder.append("- closing.dominant가 기쁨: 메이저 감성, 90–110 BPM, 밝은 가요/올드팝.\n");
        promptBuilder.append("- 슬픔/상처: 60–80 BPM, 피아노/스트링 중심 연주곡, 잔잔한 발라드.\n");
        promptBuilder.append("- 불안/당황: 앰비언트/뉴에이지/재즈 트리오, 반복적이고 안정적인 패턴.\n");
        promptBuilder.append("- 분노: 중저음 안정의 어쿠스틱/재즈 발라드(자극 최소).\n");
        promptBuilder.append("flowPattern이 U-shape면 도입(저각성)→중간(소폭 상승)→끝(안도)로 배치합니다.\n\n");
        
        promptBuilder.append("[입력]\n");
        promptBuilder.append("- DiaryPlan(JSON):\n");
        try {
            String diaryPlanJson = objectMapper.writeValueAsString(diaryPlan);
            promptBuilder.append(diaryPlanJson).append("\n\n");
        } catch (Exception e) {
            // 파싱 실패 시 기본값 사용
            promptBuilder.append("{\"flowPattern\":\"안정형\",\"closing\":{\"dominant\":\"중립\"},\"styleHints\":{\"toneStart\":\"차분한\",\"toneMid\":\"편안한\",\"toneEnd\":\"따뜻한\"}}\n\n");
        }
        
        promptBuilder.append("- Summary(JSON):\n");
        try {
            String summaryJson = objectMapper.writeValueAsString(summary);
            promptBuilder.append(summaryJson).append("\n\n");
        } catch (Exception e) {
            // 파싱 실패 시 기본값 사용
            promptBuilder.append("{\"situation\":\"대화 내용 요약\",\"events\":[],\"anchors\":{\"era\":\"1980년대\"},\"highlights\":{},\"quotes\":[]}\n\n");
        }
        
        promptBuilder.append("[출력 스키마(JSON만)]\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"recommended_music\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"노래 제목\",\n");
        promptBuilder.append("      \"artist\": \"가수 이름\",\n");
        promptBuilder.append("      \"mood\": \"음악 분위기\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}");
        
        // GPT API 호출
        GPTRequest gptRequest = new GPTRequest();
        gptRequest.setModel(defaultModel);
        gptRequest.setMax_tokens(600); // JSON 응답에 최적화
        gptRequest.setTemperature(0.3); // 정확한 아티스트/곡명을 위해 낮춤
        gptRequest.setStream(false);
        
        GPTMessage userMessage = new GPTMessage("user", promptBuilder.toString());
        
        gptRequest.setMessages(List.of(userMessage));
        
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
