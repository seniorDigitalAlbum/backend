package com.chimaenono.dearmind.conversationMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversation")
@Tag(name = "ConversationMessage", description = "대화 메시지 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class ConversationMessageController {
    
    @Autowired
    private ConversationMessageService conversationMessageService;
    
    
    @Operation(summary = "대화 세션의 모든 메시지 조회", 
               description = "특정 대화 세션의 모든 메시지를 시간순으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ConversationMessageResponse>> getMessagesByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        List<ConversationMessageResponse> messages = 
            conversationMessageService.getMessagesByConversationId(conversationId);
        return ResponseEntity.ok(messages);
    }
    
    @Operation(summary = "특정 메시지 조회", 
               description = "메시지 ID로 특정 메시지를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
    })
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<ConversationMessageResponse> getMessageById(
            @Parameter(description = "메시지 ID", example = "1")
            @PathVariable Long messageId) {
        Optional<ConversationMessageResponse> message = 
            conversationMessageService.getMessageById(messageId);
        return message.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "대화 세션의 사용자 메시지만 조회", 
               description = "특정 대화 세션의 사용자 메시지만 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 메시지 조회 성공")
    })
    @GetMapping("/{conversationId}/messages/user")
    public ResponseEntity<List<ConversationMessageResponse>> getUserMessagesByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        List<ConversationMessageResponse> messages = 
            conversationMessageService.getUserMessagesByConversationId(conversationId);
        return ResponseEntity.ok(messages);
    }
    
    @Operation(summary = "대화 세션의 AI 메시지만 조회", 
               description = "특정 대화 세션의 AI 메시지만 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 메시지 조회 성공")
    })
    @GetMapping("/{conversationId}/messages/ai")
    public ResponseEntity<List<ConversationMessageResponse>> getAIMessagesByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        List<ConversationMessageResponse> messages = 
            conversationMessageService.getAIMessagesByConversationId(conversationId);
        return ResponseEntity.ok(messages);
    }
    
    @Operation(summary = "대화 세션의 메시지 개수 조회", 
               description = "특정 대화 세션의 전체 메시지 개수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 개수 조회 성공")
    })
    @GetMapping("/{conversationId}/messages/count")
    public ResponseEntity<Long> getMessageCountByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        long count = conversationMessageService.getMessageCountByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }
    
    @Operation(summary = "대화 세션의 사용자 메시지 개수 조회", 
               description = "특정 대화 세션의 사용자 메시지 개수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 메시지 개수 조회 성공")
    })
    @GetMapping("/{conversationId}/messages/count/user")
    public ResponseEntity<Long> getUserMessageCountByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        long count = conversationMessageService.getUserMessageCountByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }
    
    @Operation(summary = "대화 세션의 AI 메시지 개수 조회", 
               description = "특정 대화 세션의 AI 메시지 개수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 메시지 개수 조회 성공")
    })
    @GetMapping("/{conversationId}/messages/count/ai")
    public ResponseEntity<Long> getAIMessageCountByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        long count = conversationMessageService.getAIMessageCountByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }
    
    @Operation(summary = "메시지 삭제", 
               description = "특정 메시지를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
    })
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @Parameter(description = "메시지 ID", example = "1")
            @PathVariable Long messageId) {
        boolean deleted = conversationMessageService.deleteMessage(messageId);
        if (deleted) {
            return ResponseEntity.ok("메시지가 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
