package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.diary.DiaryResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private MusicRecommendationService musicRecommendationService;
    
    @Autowired
    private com.chimaenono.dearmind.diary.DiaryPlanService diaryPlanService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostMapping("/start")
    @Operation(summary = "대화 시작 (통합)", description = "카메라 세션, 마이크 세션, 대화방을 통합으로 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 시작 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ConversationStartResponse> startConversation(
            @Parameter(description = "대화 시작 요청 데이터") @RequestBody ConversationStartRequest request,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        try {
            ConversationStartResponse response = conversationService.startConversation(request, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ConversationStartResponse.error("잘못된 요청: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ConversationStartResponse.error("대화 시작 중 오류가 발생했습니다: " + e.getMessage()));
        }
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
    
    @GetMapping("/user")
    @Operation(summary = "사용자별 대화 세션 목록", description = "현재 로그인한 사용자의 모든 대화 세션을 최신순으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 목록 조회 성공")
    })
    public ResponseEntity<List<Conversation>> getConversationsByUser(@AuthenticationPrincipal(expression = "id") Long userId) {
        
        List<Conversation> conversations = conversationService.getConversationsByUser(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/user/active")
    @Operation(summary = "사용자의 활성 대화 세션", description = "현재 로그인한 사용자의 활성 상태 대화 세션을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "활성 대화 세션 조회 성공"),
        @ApiResponse(responseCode = "404", description = "활성 대화 세션이 없음")
    })
    public ResponseEntity<Conversation> getActiveConversation(@AuthenticationPrincipal(expression = "id") Long userId) {
        
        Optional<Conversation> conversation = conversationService.getActiveConversationByUser(userId);
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
    
    
    
    @PostMapping("/dummy")
    @Operation(summary = "더미 대화 데이터 생성", description = "현재 로그인한 사용자에 대한 테스트용 더미 대화 세션과 메시지를 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "더미 데이터 생성 성공")
    })
    public ResponseEntity<String> createDummyConversations(@AuthenticationPrincipal(expression = "id") Long userId) {
        
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
            
            // DiaryPlan과 Summary 생성 (음악 추천용)
            com.chimaenono.dearmind.diary.DiaryPlan diaryPlan = null;
            com.chimaenono.dearmind.diary.Summary summary = null;
            
            try {
                // DiaryPlanService를 통해 DiaryPlan 생성
                if (diaryPlanService != null) {
                    diaryPlan = diaryPlanService.buildDiaryPlan(conversationId);
                }
                
                // Summary 파싱 (JSON 문자열에서 객체로 변환)
                if (conversation.getSummary() != null && objectMapper != null) {
                    summary = objectMapper.readValue(conversation.getSummary(), com.chimaenono.dearmind.diary.Summary.class);
                }
            } catch (Exception e) {
                log.warn("DiaryPlan 또는 Summary 생성 실패, 기본값 사용: {}", e.getMessage());
                // 기본값으로 대체
                diaryPlan = createDefaultDiaryPlan();
                summary = createDefaultSummary();
            }
            
            // 음악 추천 조회 또는 생성
            List<MusicRecommendation> musicRecommendations = musicRecommendationService
                .getOrGenerateMusicRecommendations(conversationId, diaryPlan, summary);
            
            DiaryResponse response = DiaryResponse.success(
                conversationId,
                conversation.getDiary(),
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
     * 기본 DiaryPlan을 생성합니다.
     */
    private com.chimaenono.dearmind.diary.DiaryPlan createDefaultDiaryPlan() {
        com.chimaenono.dearmind.diary.DiaryPlan defaultPlan = new com.chimaenono.dearmind.diary.DiaryPlan();
        defaultPlan.setFlowPattern("안정형");
        defaultPlan.setOpening(Map.of("dominant", "중립"));
        defaultPlan.setClosing(Map.of("dominant", "중립"));
        defaultPlan.setStyleHints(Map.of(
            "toneStart", "차분한",
            "toneMid", "편안한", 
            "toneEnd", "따뜻한"
        ));
        return defaultPlan;
    }
    
    /**
     * 기본 Summary를 생성합니다.
     */
    private com.chimaenono.dearmind.diary.Summary createDefaultSummary() {
        com.chimaenono.dearmind.diary.Summary defaultSummary = new com.chimaenono.dearmind.diary.Summary();
        defaultSummary.setSituation("대화 내용 요약");
        defaultSummary.setEvents(List.of());
        
        com.chimaenono.dearmind.diary.Summary.Anchors anchors = new com.chimaenono.dearmind.diary.Summary.Anchors();
        anchors.setEra("1980년대");
        anchors.setPeople(List.of());
        anchors.setPlace(List.of());
        anchors.setObjects(List.of());
        defaultSummary.setAnchors(anchors);
        
        com.chimaenono.dearmind.diary.Summary.Highlights highlights = new com.chimaenono.dearmind.diary.Summary.Highlights();
        highlights.setBestMoment("");
        highlights.setHardMoment("");
        highlights.setInsight("");
        defaultSummary.setHighlights(highlights);
        
        defaultSummary.setQuotes(List.of());
        return defaultSummary;
    }
} 