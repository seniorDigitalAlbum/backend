package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversation API", description = "AI&사용자 대화 세션 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
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
    @Operation(summary = "대화 세션 종료", description = "대화 세션을 완료 상태로 변경합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 세션 종료 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    public ResponseEntity<Conversation> endConversation(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        Conversation conversation = conversationService.endConversation(conversationId);
        if (conversation != null) {
            return ResponseEntity.ok(conversation);
        } else {
            return ResponseEntity.notFound().build();
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
            @Parameter(description = "메시지 내용", example = "안녕하세요") @RequestParam String content,
            @Parameter(description = "음성 파일 경로") @RequestParam(required = false) String audioFilePath,
            @Parameter(description = "비디오 파일 경로") @RequestParam(required = false) String videoFilePath) {
        
        // 대화 세션이 존재하는지 확인
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationMessage message = conversationService.saveUserMessage(conversationId, content, audioFilePath, videoFilePath);
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
            @Parameter(description = "메시지 내용", example = "안녕하세요! 오늘은 어떤 이야기를 나누고 싶으신가요?") @RequestParam String content,
            @Parameter(description = "음성 파일 경로") @RequestParam(required = false) String audioFilePath,
            @Parameter(description = "비디오 파일 경로") @RequestParam(required = false) String videoFilePath) {
        
        // 대화 세션이 존재하는지 확인
        Optional<Conversation> conversation = conversationService.getConversationById(conversationId);
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationMessage message = conversationService.saveAIMessage(conversationId, content, audioFilePath, videoFilePath);
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
    
    @GetMapping("/health")
    @Operation(summary = "대화 서비스 상태 확인", description = "대화 서비스의 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Conversation service is running");
    }
} 