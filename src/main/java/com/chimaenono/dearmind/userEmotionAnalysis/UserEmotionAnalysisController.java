package com.chimaenono.dearmind.userEmotionAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/emotion-analysis")
@Tag(name = "UserEmotionAnalysis", description = "사용자 감정 분석 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class UserEmotionAnalysisController {
    
    @Autowired
    private UserEmotionAnalysisService userEmotionAnalysisService;
    
    @Autowired
    private CombineEmotionService combineEmotionService;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    
    @Operation(summary = "특정 메시지의 감정 분석 결과 조회", 
               description = "대화 메시지 ID로 감정 분석 결과를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 분석 결과 조회 성공"),
        @ApiResponse(responseCode = "404", description = "감정 분석 결과를 찾을 수 없음")
    })
    @GetMapping("/message/{conversationMessageId}")
    public ResponseEntity<UserEmotionAnalysisResponse> getEmotionAnalysisByMessageId(
            @Parameter(description = "대화 메시지 ID", example = "123")
            @PathVariable Long conversationMessageId) {
        Optional<UserEmotionAnalysisResponse> analysis = 
            userEmotionAnalysisService.getEmotionAnalysisByMessageId(conversationMessageId);
        return analysis.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "대화 세션의 모든 감정 분석 결과 조회", 
               description = "특정 대화 세션의 모든 감정 분석 결과를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 분석 결과 목록 조회 성공")
    })
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<UserEmotionAnalysisResponse>> getEmotionAnalysesByConversationId(
            @Parameter(description = "대화 세션 ID", example = "1")
            @PathVariable Long conversationId) {
        List<UserEmotionAnalysisResponse> analyses = 
            userEmotionAnalysisService.getEmotionAnalysesByConversationId(conversationId);
        return ResponseEntity.ok(analyses);
    }
    
    @Operation(summary = "특정 감정으로 필터링된 감정 분석 결과 조회", 
               description = "통합 감정으로 필터링된 감정 분석 결과를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정별 분석 결과 조회 성공")
    })
    @GetMapping("/emotion/{emotion}")
    public ResponseEntity<List<UserEmotionAnalysisResponse>> getEmotionAnalysesByEmotion(
            @Parameter(description = "감정", example = "joy")
            @PathVariable String emotion) {
        List<UserEmotionAnalysisResponse> analyses = 
            userEmotionAnalysisService.getEmotionAnalysesByEmotion(emotion);
        return ResponseEntity.ok(analyses);
    }
    
    @Operation(summary = "신뢰도 범위로 필터링된 감정 분석 결과 조회", 
               description = "최소 신뢰도 이상의 감정 분석 결과를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "신뢰도별 분석 결과 조회 성공")
    })
    @GetMapping("/confidence")
    public ResponseEntity<List<UserEmotionAnalysisResponse>> getEmotionAnalysesByMinConfidence(
            @Parameter(description = "최소 신뢰도", example = "0.8")
            @RequestParam Double minConfidence) {
        List<UserEmotionAnalysisResponse> analyses = 
            userEmotionAnalysisService.getEmotionAnalysesByMinConfidence(minConfidence);
        return ResponseEntity.ok(analyses);
    }
    
    
    @Operation(summary = "감정 분석 결과 삭제", 
               description = "특정 감정 분석 결과를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 분석 결과 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "감정 분석 결과를 찾을 수 없음")
    })
    @DeleteMapping("/{analysisId}")
    public ResponseEntity<String> deleteEmotionAnalysis(
            @Parameter(description = "감정 분석 ID", example = "1")
            @PathVariable Long analysisId) {
        boolean deleted = userEmotionAnalysisService.deleteEmotionAnalysis(analysisId);
        if (deleted) {
            return ResponseEntity.ok("감정 분석 결과가 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/facial")
    @Operation(summary = "표정 감정 분석 결과 저장", 
               description = "YOLO API에서 분석된 표정 감정 결과를 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "표정 감정 분석 결과 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "대화 메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<UserEmotionAnalysisResponse> saveFacialEmotionAnalysis(
            @Valid @RequestBody FacialEmotionSaveRequest request) {
        try {
            UserEmotionAnalysisResponse response = userEmotionAnalysisService.saveFacialEmotionAnalysis(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/speech")
    @Operation(summary = "말 감정 분석 결과 저장", 
               description = "외부 API에서 분석된 말 감정 결과를 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "말 감정 분석 결과 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "대화 메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<UserEmotionAnalysisResponse> saveSpeechEmotionAnalysis(
            @Valid @RequestBody SpeechEmotionSaveRequest request) {
        try {
            UserEmotionAnalysisResponse response = userEmotionAnalysisService.saveSpeechEmotionAnalysis(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/combine")
    @Operation(summary = "통합 감정 계산 및 저장", 
               description = "표정 감정과 말 감정을 통합하여 최종 감정을 계산하고 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "통합 감정 계산 및 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "감정 분석 데이터를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> combineEmotions(
            @Valid @RequestBody CombineEmotionRequest request) {
        try {
            UserEmotionAnalysisResponse response = combineEmotionService.combineEmotions(request.getConversationMessageId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("감정 통합 계산 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "error", "감정 통합 계산 실패",
                    "message", e.getMessage(),
                    "conversationMessageId", request.getConversationMessageId()
                ));
        } catch (Exception e) {
            log.error("감정 통합 계산 중 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "서버 내부 오류",
                    "message", e.getMessage(),
                    "conversationMessageId", request.getConversationMessageId()
                ));
        }
    }
    
    @Operation(summary = "감정 분석 서비스 상태 확인", 
               description = "감정 분석 서비스의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Emotion Analysis service is running");
    }
    
    @Operation(summary = "감정 분석 결과 조회", 
               description = "conversationMessageId로 감정 분석 결과를 조회합니다.")
    @GetMapping("/{conversationMessageId}")
    public ResponseEntity<?> getEmotionAnalysis(
            @Parameter(description = "대화 메시지 ID", required = true)
            @PathVariable Long conversationMessageId) {
        
        try {
            Optional<UserEmotionAnalysis> analysis = userEmotionAnalysisRepository
                    .findByConversationMessageId(conversationMessageId);
            
            if (analysis.isPresent()) {
                UserEmotionAnalysis result = analysis.get();
                
                // combinedDistribution이 null이 아닌지 확인
                String distributionStatus = result.getCombinedDistribution() != null ? 
                    "✅ 저장됨" : "❌ null";
                
                Map<String, Object> response = Map.of(
                    "id", result.getId(),
                    "conversationMessageId", conversationMessageId,
                    "combinedEmotion", result.getCombinedEmotion() != null ? result.getCombinedEmotion() : "null",
                    "combinedConfidence", result.getCombinedConfidence() != null ? result.getCombinedConfidence() : "null",
                    "combinedDistribution", result.getCombinedDistribution() != null ? result.getCombinedDistribution() : "null",
                    "distributionStatus", distributionStatus,
                    "analysisTimestamp", result.getAnalysisTimestamp()
                );
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "감정 분석 결과를 찾을 수 없습니다.", 
                                   "conversationMessageId", conversationMessageId));
            }
            
        } catch (Exception e) {
            log.error("감정 분석 결과 조회 중 오류 발생: conversationMessageId={}", conversationMessageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "감정 분석 결과 조회 중 오류가 발생했습니다.", 
                               "message", e.getMessage()));
        }
    }
}
