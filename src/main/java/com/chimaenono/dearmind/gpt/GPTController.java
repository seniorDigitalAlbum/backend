package com.chimaenono.dearmind.gpt;

import com.chimaenono.dearmind.conversation.ConversationContextService;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageResponse;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageService;
import com.chimaenono.dearmind.tts.TTSRequest;
import com.chimaenono.dearmind.tts.TTSService;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/gpt")
@Tag(name = "GPT", description = "GPT API를 활용한 감정 기반 대화 생성")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class GPTController {
    
    @Autowired
    private GPTService gptService;
    
    @Autowired
    private ConversationContextService conversationContextService;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    @Autowired
    private ConversationMessageService conversationMessageService;
    
    
    @Autowired
    private TTSService ttsService;
    
    @PostMapping("/generate")
    @Operation(summary = "감정 기반 대화 생성", 
               description = "사용자의 감정 분석 결과를 바탕으로 GPT API를 통해 공감적인 대화 응답을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "대화 메시지 또는 감정 분석 데이터를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationGenerateResponse> generateConversation(
            @Valid @RequestBody ConversationGenerateRequest request) {
        try {
            // 대화 컨텍스트 조회
            var contextResponse = conversationContextService.getConversationContext(request.getConversationMessageId());
            if (!contextResponse.getSuccess()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ConversationGenerateResponse.error("대화 컨텍스트를 찾을 수 없습니다: " + contextResponse.getMessage()));
            }
            
            // 감정 분석 데이터 조회
            Optional<UserEmotionAnalysis> emotionAnalysisOpt = userEmotionAnalysisRepository
                    .findByConversationMessageId(request.getConversationMessageId());
            
            String emotion = "neutral";
            Double confidence = 0.5;
            
            if (emotionAnalysisOpt.isPresent()) {
                UserEmotionAnalysis analysis = emotionAnalysisOpt.get();
                emotion = analysis.getCombinedEmotion() != null ? analysis.getCombinedEmotion() : "neutral";
                confidence = analysis.getCombinedConfidence() != null ? analysis.getCombinedConfidence() : 0.5;
            }
            
            // GPT API를 통한 응답 생성
            String aiResponse = gptService.generateEmotionBasedResponse(
                    emotion,
                    confidence,
                    contextResponse.getPrevUser(),
                    contextResponse.getPrevSys(),
                    contextResponse.getCurrUser()
            );
            
            // AI 응답을 ConversationMessage에 저장
            ConversationMessageResponse savedAIMessage = conversationMessageService.saveAIMessage(
                    contextResponse.getConversationId(), aiResponse);
            
            // TTS 변환 (파일 저장 없이 Base64로 반환)
            String audioBase64 = null;
            try {
                TTSRequest ttsRequest = new TTSRequest();
                ttsRequest.setText(aiResponse);
                ttsRequest.setLanguageCode("ko-KR");
                ttsRequest.setVoiceName("ko-KR-Wavenet-A");
                ttsRequest.setAudioEncoding("MP3");
                
                var ttsResponse = ttsService.convertToSpeech(ttsRequest);
                if (ttsResponse.isSuccess() && ttsResponse.getAudioData() != null) {
                    // TTSResponse의 audioData는 이미 Base64로 인코딩된 데이터
                    audioBase64 = ttsResponse.getAudioData();
                }
            } catch (Exception e) {
                System.err.println("TTS 변환 실패: " + e.getMessage());
                // TTS 실패해도 GPT 응답은 정상 반환
            }
            
            String emotionInfo = emotion + " (" + (int)(confidence * 100) + "%)";
            
            ConversationGenerateResponse response = ConversationGenerateResponse.success(
                    aiResponse, emotionInfo, request.getConversationMessageId(), 
                    savedAIMessage.getId(), audioBase64);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConversationGenerateResponse.error("대화 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/test")
    @Operation(summary = "GPT API 테스트", 
               description = "GPT API 연결을 테스트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GPT API 테스트 성공"),
        @ApiResponse(responseCode = "500", description = "GPT API 호출 실패")
    })
    public ResponseEntity<GPTResponse> testGPT(@Valid @RequestBody GPTRequest request) {
        try {
            GPTResponse response = gptService.generateResponse(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/emotion-prompt-test")
    @Operation(summary = "감정 기반 프롬프트 테스트", 
               description = "generateEmotionBasedResponse 함수를 직접 테스트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 기반 프롬프트 테스트 성공"),
        @ApiResponse(responseCode = "500", description = "프롬프트 테스트 실패")
    })
    public ResponseEntity<Map<String, Object>> testEmotionPrompt(@RequestBody Map<String, Object> request) {
        try {
            String userText = (String) request.get("userText");
            String emotion = (String) request.get("emotion");
            Double confidence = Double.valueOf(request.get("confidence").toString());
            String prevContext = (String) request.getOrDefault("prevContext", "");
            
            // prevContext를 prevUser, prevSys, currUser로 분리
            String prevUser = "";
            String prevSys = "";
            
            if (!prevContext.isEmpty()) {
                String[] lines = prevContext.split("\n");
                for (String line : lines) {
                    if (line.startsWith("사용자:") || line.startsWith("USER:")) {
                        prevUser = line.substring(line.indexOf(":") + 1).trim();
                    } else if (line.startsWith("AI:") || line.startsWith("시스템:")) {
                        prevSys = line.substring(line.indexOf(":") + 1).trim();
                    }
                }
            }
            
            // generateEmotionBasedResponse 함수 직접 호출
            String aiResponse = gptService.generateEmotionBasedResponse(
                emotion, confidence, prevUser, prevSys, userText
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("aiResponse", aiResponse);
            response.put("emotion", emotion);
            response.put("confidence", confidence);
            response.put("userText", userText);
            response.put("prevUser", prevUser);
            response.put("prevSys", prevSys);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "감정 기반 프롬프트 테스트 실패");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/diary-prompt-test")
    @Operation(summary = "일기 생성 프롬프트 테스트", 
               description = "generateAndSaveDiary 함수의 프롬프트를 직접 테스트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "일기 생성 프롬프트 테스트 성공"),
        @ApiResponse(responseCode = "500", description = "일기 생성 테스트 실패")
    })
    public ResponseEntity<Map<String, Object>> testDiaryPrompt(@RequestBody Map<String, Object> request) {
        try {
            String summaryJson = (String) request.get("summaryJson");
            String diaryPlanJson = (String) request.get("diaryPlanJson");
            
            // 프롬프트 구성 (generateAndSaveDiary와 동일한 로직)
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("당신은 회상요법을 돕는 공감형 '일기 작가'입니다.\n");
            promptBuilder.append("목표: 제공된 요약(JSON)과 DiaryPlan만을 근거로, 한국어 1인칭 과거형 일기를 작성합니다.\n");
            promptBuilder.append("규칙:\n");
            promptBuilder.append("- 사실 준수: 요약에 없는 새로운 사실을 만들지 마세요.\n");
            promptBuilder.append("- 문체: 쉬운 단어, 짧은 문장(한 문장 10~15단어), 과장·훈계 금지.\n");
            promptBuilder.append("- 구조: 3단락(각 3~4문장). 시작-전환-마무리의 정서 흐름을 DiaryPlan에 맞게 구성.\n");
            promptBuilder.append("- 정서: DiaryPlan.styleHints(toneStart/mid/end)에 맞는 어조 사용.\n\n");
            
            promptBuilder.append("출력 형식:\n");
            promptBuilder.append("- 제목 1줄 + 본문. JSON 금지, 추가 설명 금지.\n\n");
            
            promptBuilder.append("다음 입력으로 일기를 작성하세요.\n\n");
            
            promptBuilder.append("[요약(JSON)]\n");
            promptBuilder.append(summaryJson).append("\n");
            promptBuilder.append("// 스키마 예시:\n");
            promptBuilder.append("// {\"meta\":{\"date\":\"2025-09-16\",\"locale\":\"ko-KR\"},\n");
            promptBuilder.append("//  \"situation\":\"...\",\n");
            promptBuilder.append("//  \"events\":[\"...\",\"...\",\"...\"],\n");
            promptBuilder.append("//  \"anchors\":{\"people\":[\"가족\"],\"place\":[\"동네 골목\"],\"era\":\"초등학교 시절\",\"objects\":[\"라디오\"]},\n");
            promptBuilder.append("//  \"highlights\":{\"best_moment\":\"...\",\"hard_moment\":\"...\",\"insight\":\"...\"},\n");
            promptBuilder.append("//  \"quotes\":[\"사용자 원문 1문장\"]}\n\n");
            
            promptBuilder.append("[DiaryPlan(JSON)]\n");
            promptBuilder.append(diaryPlanJson).append("\n");
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
            gptRequest.setModel("gpt-4o-mini"); // 현재 기본 모델 사용
            gptRequest.setMax_tokens(600);
            gptRequest.setTemperature(0.4);
            gptRequest.setStream(false);
            
            // 메시지 구성
            GPTMessage userMessage = new GPTMessage("user", promptBuilder.toString());
            gptRequest.setMessages(List.of(userMessage));
            
            // GPT API 호출
            GPTResponse gptResponse = gptService.generateResponse(gptRequest);
            
            if (gptResponse.getChoices() == null || gptResponse.getChoices().isEmpty()) {
                throw new RuntimeException("GPT API 응답에 선택지가 없습니다.");
            }
            
            String diary = gptResponse.getChoices().get(0).getMessage().getContent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("diary", diary);
            response.put("summaryJson", summaryJson);
            response.put("diaryPlanJson", diaryPlanJson);
            response.put("fullPrompt", promptBuilder.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "일기 생성 프롬프트 테스트 실패");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/emotion-test")
    @Operation(summary = "감정 기반 대화 테스트", 
               description = "감정 기반 대화 생성을 테스트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 기반 대화 테스트 성공"),
        @ApiResponse(responseCode = "500", description = "대화 생성 실패")
    })
    public ResponseEntity<ConversationGenerateResponse> testEmotionBasedConversation(
            @Parameter(description = "감정", example = "슬픔") @RequestParam(defaultValue = "슬픔") String emotion,
            @Parameter(description = "신뢰도", example = "0.95") @RequestParam(defaultValue = "0.95") Double confidence,
            @Parameter(description = "이전 사용자 발화", example = "오늘 중요한 시험을 망친 것 같아요.") 
            @RequestParam(defaultValue = "오늘 중요한 시험을 망친 것 같아요.") String prevUser,
            @Parameter(description = "이전 AI 발화", example = "아이고, 정말 힘들었겠어요.") 
            @RequestParam(defaultValue = "아이고, 정말 힘들었겠어요.") String prevSys,
            @Parameter(description = "현재 사용자 발화", example = "공부한 만큼 결과가 안 나와서 실망이 커요.") 
            @RequestParam(defaultValue = "공부한 만큼 결과가 안 나와서 실망이 커요.") String currUser) {
        try {
            String aiResponse = gptService.generateEmotionBasedResponse(emotion, confidence, prevUser, prevSys, currUser);
            
            // TTS 변환 (파일 저장 없이 Base64로 반환)
            String audioBase64 = null;
            try {
                TTSRequest ttsRequest = new TTSRequest();
                ttsRequest.setText(aiResponse);
                ttsRequest.setLanguageCode("ko-KR");
                ttsRequest.setVoiceName("ko-KR-Wavenet-A");
                ttsRequest.setAudioEncoding("MP3");
                
                var ttsResponse = ttsService.convertToSpeech(ttsRequest);
                if (ttsResponse.isSuccess() && ttsResponse.getAudioData() != null) {
                    // TTSResponse의 audioData는 이미 Base64로 인코딩된 데이터
                    audioBase64 = ttsResponse.getAudioData();
                }
            } catch (Exception e) {
                System.err.println("TTS 변환 실패: " + e.getMessage());
                // TTS 실패해도 GPT 응답은 정상 반환
            }
            
            String emotionInfo = emotion + " (" + (int)(confidence * 100) + "%)";
            
            ConversationGenerateResponse response = ConversationGenerateResponse.success(
                    aiResponse, emotionInfo, 0L, 0L, audioBase64);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConversationGenerateResponse.error("대화 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/conversation-summary")
    @Operation(summary = "대화 내용 요약", description = "GPT를 사용하여 대화 내용을 요약합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 요약 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationSummaryResponse> generateConversationSummary(
            @Valid @RequestBody ConversationSummaryRequest request) {
        try {
            String summary = gptService.generateConversationSummary(
                request.getConversationId(), 
                request.getSummaryLength()
            );
            
            ConversationSummaryResponse response = ConversationSummaryResponse.success(
                request.getConversationId(),
                summary,
                request.getSummaryLength()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConversationSummaryResponse.error("대화 요약 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/generate-simple")
    @Operation(summary = "KoBERT 테스트용 단순 대화 생성", 
               description = "KoBERT 테스트 페이지에서 사용하는 회상 요법 기반 대화 생성 (conversationMessageId 불필요, 저장/TTS 없음)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> generateSimpleConversation(
            @Parameter(description = "사용자 감정", example = "기쁨") @RequestParam String emotion,
            @Parameter(description = "감정 신뢰도", example = "0.85") @RequestParam Double confidence,
            @Parameter(description = "이전 사용자 발화", example = "오늘 힘든 하루였어요.") 
            @RequestParam(required = false) String prevUser,
            @Parameter(description = "이전 AI 발화", example = "정말 힘들었겠어요.") 
            @RequestParam(required = false) String prevSys,
            @Parameter(description = "현재 사용자 발화", example = "공부한 만큼 결과가 안 나와서 실망이 커요.") 
            @RequestParam String currUser) {
        try {
            // GPT API를 통한 응답 생성 (저장, TTS 없이 단순 생성만)
            String aiResponse = gptService.generateEmotionBasedResponse(
                emotion, 
                confidence, 
                prevUser, 
                prevSys, 
                currUser
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("aiResponse", aiResponse);
            response.put("emotion", emotion);
            response.put("confidence", confidence);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "대화 생성 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
