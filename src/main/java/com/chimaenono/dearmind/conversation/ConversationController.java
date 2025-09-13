package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;
import com.chimaenono.dearmind.music.MusicRecommendation;
import com.chimaenono.dearmind.music.MusicRecommendationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversation API", description = "AI&사용자 대화 세션 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private ConversationContextService conversationContextService;
    
    @Autowired
    private AsyncService asyncService;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    @Autowired
    private MusicRecommendationService musicRecommendationService;
    
    @PostMapping("/start")
    @Operation(summary = "대화 시작 (통합)", description = "카메라 세션, 마이크 세션, 대화방을 통합으로 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 시작 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationStartResponse> startConversation(
            @Parameter(description = "대화 시작 요청 데이터") @RequestBody ConversationStartRequest request) {
        
        try {
            ConversationStartResponse response = conversationService.startConversation(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ConversationStartResponse.error("잘못된 요청: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ConversationStartResponse.error("대화 시작 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping
    @Operation(summary = "대화 세션 생성", description = "새로운 대화 세션을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Conversation> createConversation(
            @Parameter(description = "사용자 ID", example = "user123") @RequestParam String userId,
            @Parameter(description = "질문 ID", example = "1") @RequestParam Long questionId,
            @Parameter(description = "카메라 세션 ID", example = "camera_session_123") @RequestParam String cameraSessionId,
            @Parameter(description = "마이크 세션 ID", example = "microphone_session_456") @RequestParam String microphoneSessionId) {
        
        Conversation conversation = conversationService.createConversation(userId, questionId, cameraSessionId, microphoneSessionId);
        return ResponseEntity.ok(conversation);
    }
    
    @GetMapping("/{conversationId}")
    @Operation(summary = "대화 세션 조회", description = "ID로 대화 세션을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<Conversation> getConversation(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        return conversation.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 대화 세션 목록", description = "사용자의 모든 대화 세션을 최신순으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 목록 조회 성공")
    })
    public ResponseEntity<List<Conversation>> getConversationsByUser(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        List<Conversation> conversations = conversationService.getConversationsByUserId(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/user/{userId}/active")
    @Operation(summary = "사용자의 활성 대화 세션", description = "사용자의 활성 상태 대화 세션을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "활성 대화 세션 조회 성공"),
        @ApiResponse(responseCode = "404", description = "활성 대화 세션이 없음")
    })
    public ResponseEntity<Conversation> getActiveConversation(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        Optional<Conversation> conversation = conversationService.getActiveConversationByUserId(userId);
        return conversation.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/question/{questionId}")
    @Operation(summary = "질문별 대화 세션 목록", description = "특정 질문에 대한 모든 대화 세션을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 목록 조회 성공")
    })
    public ResponseEntity<List<Conversation>> getConversationsByQuestion(
            @Parameter(description = "질문 ID", example = "1") @PathVariable Long questionId) {
        
        List<Conversation> conversations = conversationService.getConversationsByQuestionId(questionId);
        return ResponseEntity.ok(conversations);
    }
    
    @PutMapping("/{conversationId}/status")
    @Operation(summary = "대화 세션 상태 업데이트", description = "대화 세션의 상태를 업데이트합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<Conversation> updateConversationStatus(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId,
            @Parameter(description = "새로운 상태", example = "COMPLETED") @RequestParam Conversation.ConversationStatus status) {
        
        Conversation conversation = conversationService.updateConversationStatus(conversationId, status);
        if (conversation != null) {
            return ResponseEntity.ok(conversation);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{conversationId}/end")
    @Operation(summary = "대화 세션 종료", description = "대화 세션을 완료 상태로 변경하고 백그라운드에서 요약 및 일기 생성을 시작합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 종료 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationEndResponse> endConversation(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        try {
            // 대화 종료 및 백그라운드 처리 시작
            Conversation conversation = conversationService.endConversationAndStartProcessing(conversationId);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 대화 메시지 조회
            List<ConversationMessage> messages = conversationService.getMessagesByConversationId(conversationId);
            
            // 백그라운드에서 요약 및 일기 생성 시작
            asyncService.generateSummaryAndDiary(conversationId);
            
            // 응답 생성
            ConversationEndResponse response = ConversationEndResponse.success(
                conversationId,
                conversation.getStatus().toString(),
                conversation.getProcessingStatus().toString(),
                messages,
                "일기 생성 중입니다..."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ConversationEndResponse.error("대화 종료 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "대화 메시지 목록", description = "특정 대화 세션의 모든 메시지를 시간순으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<List<ConversationMessage>> getConversationMessages(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        // 대화 세션이 존재하는지 확인
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<ConversationMessage> messages = conversationService.getMessagesByConversationId(conversationId);
        return ResponseEntity.ok(messages);
    }
    
    @PostMapping("/{conversationId}/messages/user")
    @Operation(summary = "사용자 메시지 저장", description = "사용자의 메시지를 저장합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 저장 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<ConversationMessage> saveUserMessage(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId,
            @Parameter(description = "메시지 내용", example = "안녕하세요") @RequestParam String content) {
        
        // 대화 세션이 존재하는지 확인
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationMessage message = conversationService.saveUserMessage(conversationId, content);
        return ResponseEntity.ok(message);
    }
    
    @PostMapping("/{conversationId}/messages/ai")
    @Operation(summary = "AI 메시지 저장", description = "AI의 메시지를 저장합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 저장 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<ConversationMessage> saveAIMessage(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId,
            @Parameter(description = "메시지 내용", example = "안녕하세요! 오늘은 어떤 이야기를 나누고 싶으신가요?") @RequestParam String content) {
        
        // 대화 세션이 존재하는지 확인
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationMessage message = conversationService.saveAIMessage(conversationId, content);
        return ResponseEntity.ok(message);
    }
    
    @PostMapping("/dummy/{userId}")
    @Operation(summary = "더미 대화 데이터 생성", description = "테스트용 더미 대화 세션과 메시지를 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "더미 데이터 생성 성공")
    })
    public ResponseEntity<String> createDummyConversations(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        conversationService.createDummyConversations(userId);
        return ResponseEntity.ok("더미 대화 데이터가 성공적으로 생성되었습니다.");
    }
    
    @GetMapping("/context/{conversationMessageId}")
    @Operation(summary = "대화 컨텍스트 조회", description = "특정 메시지의 이전 대화 컨텍스트를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 컨텍스트 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationContextResponse> getConversationContext(
            @Parameter(description = "대화 메시지 ID", example = "123") @PathVariable Long conversationMessageId) {
        
        ConversationContextResponse response = conversationContextService.getConversationContext(conversationMessageId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/{conversationId}/summary")
    @Operation(summary = "대화 요약 정보 조회", description = "대화 세션의 요약 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 요약 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationSummaryResponse> getConversationSummary(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        try {
            ConversationSummaryResponse summary = conversationService.getConversationSummary(conversationId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/{conversationId}/processing-status")
    @Operation(summary = "처리 상태 확인", description = "대화의 요약 및 일기 생성 처리 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "처리 상태 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ProcessingStatusResponse> getProcessingStatus(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        try {
            Conversation.ProcessingStatus status = conversationService.getProcessingStatus(conversationId);
            
            // 대화 세션이 존재하는지 확인
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Conversation conversation = conversationOpt.get();
            Boolean summaryCompleted = conversation.getSummary() != null && !conversation.getSummary().isEmpty();
            Boolean diaryCompleted = conversation.getDiary() != null && !conversation.getDiary().isEmpty();
            
            String message = switch (status) {
                case READY -> "처리 준비 중입니다.";
                case PROCESSING -> "요약 및 일기 생성 중입니다...";
                case COMPLETED -> "처리가 완료되었습니다.";
                case ERROR -> "처리 중 오류가 발생했습니다.";
            };
            
            ProcessingStatusResponse response = ProcessingStatusResponse.success(
                conversationId, status.toString(), summaryCompleted, diaryCompleted, message
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ProcessingStatusResponse.error("처리 상태 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{conversationId}/diary")
    @Operation(summary = "일기 조회", description = "생성된 일기와 요약 내용을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "일기 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<DiaryResponse> getDiary(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        try {
            // 대화 세션 조회
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Conversation conversation = conversationOpt.get();
            
            // 일기가 생성되지 않은 경우
            if (conversation.getDiary() == null || conversation.getDiary().isEmpty()) {
                return ResponseEntity.status(400)
                        .body(DiaryResponse.error("일기가 아직 생성되지 않았습니다."));
            }
            
            // 저장된 감정 분석 결과 사용
            DiaryResponse.EmotionSummary emotionSummary = createEmotionSummaryFromConversation(conversation);
            
            // 음악 추천 조회 또는 생성
            List<MusicRecommendation> musicRecommendations = musicRecommendationService
                .getOrGenerateMusicRecommendations(
                    conversationId,
                    conversation.getDiary(),
                    conversation.getDominantEmotion(),
                    conversation.getEmotionConfidence()
                );
            
            DiaryResponse response = DiaryResponse.success(
                conversationId,
                conversation.getSummary(),
                conversation.getDiary(),
                emotionSummary,
                musicRecommendations,
                "일기를 성공적으로 조회했습니다."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DiaryResponse.error("일기 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "대화 서비스 상태 확인", description = "대화 서비스의 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Conversation service is running");
    }
    
    /**
     * 감정 분석 결과를 요약합니다.
     */
    private DiaryResponse.EmotionSummary createEmotionSummary(List<UserEmotionAnalysis> emotions) {
        if (emotions.isEmpty()) {
            DiaryResponse.EmotionSummary summary = new DiaryResponse.EmotionSummary();
            summary.setDominantEmotion("중립");
            summary.setEmotionCounts(Map.of());
            summary.setAverageConfidence(0.0);
            summary.setAnalyzedMessageCount(0);
            return summary;
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
        
        DiaryResponse.EmotionSummary summary = new DiaryResponse.EmotionSummary();
        summary.setDominantEmotion(dominantEmotion);
        summary.setEmotionCounts(emotionCounts);
        summary.setAverageConfidence(averageConfidence);
        summary.setAnalyzedMessageCount(emotions.size());
        
        return summary;
    }
    
    /**
     * Conversation에 저장된 감정 분석 결과를 EmotionSummary로 변환합니다.
     */
    private DiaryResponse.EmotionSummary createEmotionSummaryFromConversation(Conversation conversation) {
        DiaryResponse.EmotionSummary summary = new DiaryResponse.EmotionSummary();
        
        if (conversation.getDominantEmotion() != null) {
            summary.setDominantEmotion(conversation.getDominantEmotion());
        } else {
            summary.setDominantEmotion("중립");
        }
        
        if (conversation.getEmotionConfidence() != null) {
            summary.setAverageConfidence(conversation.getEmotionConfidence());
        } else {
            summary.setAverageConfidence(0.0);
        }
        
        if (conversation.getEmotionDistribution() != null && !conversation.getEmotionDistribution().isEmpty()) {
            // JSON 문자열을 Map으로 파싱
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Integer> emotionCounts = objectMapper.readValue(
                    conversation.getEmotionDistribution(), 
                    new TypeReference<Map<String, Integer>>() {}
                );
                summary.setEmotionCounts(emotionCounts);
            } catch (Exception e) {
                System.err.println("감정 분포 JSON 파싱 오류: " + e.getMessage());
                summary.setEmotionCounts(Map.of());
            }
        } else {
            summary.setEmotionCounts(Map.of());
        }
        
        // 분석된 메시지 수는 감정 분포의 총합으로 계산
        int totalMessages = summary.getEmotionCounts().values().stream().mapToInt(Integer::intValue).sum();
        summary.setAnalyzedMessageCount(totalMessages);
        
        return summary;
    }
} 