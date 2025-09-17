package com.chimaenono.dearmind.conversation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/emotion-flow/test")
@Tag(name = "EmotionFlow Test", description = "감정 흐름 분석 테스트 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class EmotionFlowTestController {

    @Autowired
    private EmotionFlowService emotionFlowService;
    
    @Autowired
    private ConversationRepository conversationRepository;

    @Operation(summary = "감정 흐름 계산 및 저장 테스트", 
               description = "특정 conversationId에 대해 감정 흐름을 계산하고 DB에 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 흐름 계산 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/compute/{conversationId}")
    public ResponseEntity<?> computeEmotionFlow(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        
        try {
            log.info("감정 흐름 계산 시작: conversationId={}", conversationId);
            
            // 감정 흐름 계산 및 저장
            emotionFlowService.computeAndSaveFlow(conversationId);
            
            log.info("감정 흐름 계산 완료: conversationId={}", conversationId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "감정 흐름이 성공적으로 계산되고 저장되었습니다.",
                "conversationId", conversationId
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("대화 세션을 찾을 수 없음: conversationId={}", conversationId, e);
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "대화 세션을 찾을 수 없습니다.",
                "conversationId", conversationId
            ));
        } catch (Exception e) {
            log.error("감정 흐름 계산 중 오류 발생: conversationId={}", conversationId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "감정 흐름 계산 중 오류가 발생했습니다.",
                "message", e.getMessage(),
                "conversationId", conversationId
            ));
        }
    }

    @Operation(summary = "감정 흐름 결과 조회", 
               description = "특정 conversationId의 감정 흐름 분석 결과를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 흐름 조회 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음")
    })
    @GetMapping("/result/{conversationId}")
    public ResponseEntity<?> getEmotionFlowResult(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        
        try {
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "대화 세션을 찾을 수 없습니다.",
                    "conversationId", conversationId
                ));
            }
            
            Conversation conversation = conversationOpt.get();
            
            // flowPattern과 emotionFlow 상태 확인
            String flowPatternStatus = conversation.getFlowPattern() != null ? "✅ 저장됨" : "❌ null";
            String emotionFlowStatus = conversation.getEmotionFlow() != null ? "✅ 저장됨" : "❌ null";
            
            Map<String, Object> response = Map.of(
                "success", true,
                "conversationId", conversationId,
                "flowPattern", conversation.getFlowPattern() != null ? conversation.getFlowPattern() : "null",
                "flowPatternStatus", flowPatternStatus,
                "emotionFlow", conversation.getEmotionFlow() != null ? conversation.getEmotionFlow() : "null",
                "emotionFlowStatus", emotionFlowStatus,
                "message", "감정 흐름 결과 조회 완료"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("감정 흐름 결과 조회 중 오류 발생: conversationId={}", conversationId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "감정 흐름 결과 조회 중 오류가 발생했습니다.",
                "message", e.getMessage(),
                "conversationId", conversationId
            ));
        }
    }

    @Operation(summary = "서비스 상태 확인", description = "EmotionFlow 테스트 서비스 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("EmotionFlow Test service is running");
    }
}
