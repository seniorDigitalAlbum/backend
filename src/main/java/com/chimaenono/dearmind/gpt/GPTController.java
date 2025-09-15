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

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

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
