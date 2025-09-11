package com.chimaenono.userEmotionAnalysis;

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
import java.util.Optional;

@RestController
@RequestMapping("/api/emotion-analysis")
@Tag(name = "UserEmotionAnalysis", description = "사용자 감정 분석 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class UserEmotionAnalysisController {
    
    @Autowired
    private UserEmotionAnalysisService userEmotionAnalysisService;
    
    @Operation(summary = "사용자 감정 분석 결과 저장", 
               description = "표정 및 말 감정 분석 결과를 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "감정 분석 결과 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "404", description = "대화 메시지를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResponseEntity<UserEmotionAnalysisResponse> saveEmotionAnalysis(
            @Valid @RequestBody UserEmotionAnalysisRequest request) {
        try {
            UserEmotionAnalysisResponse response = userEmotionAnalysisService.saveEmotionAnalysis(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
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
    
    @Operation(summary = "감정 분석 결과 업데이트", 
               description = "기존 감정 분석 결과를 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "감정 분석 결과 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "감정 분석 결과를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PutMapping("/{analysisId}")
    public ResponseEntity<UserEmotionAnalysisResponse> updateEmotionAnalysis(
            @Parameter(description = "감정 분석 ID", example = "1")
            @PathVariable Long analysisId,
            @Valid @RequestBody UserEmotionAnalysisRequest request) {
        try {
            Optional<UserEmotionAnalysisResponse> response = 
                userEmotionAnalysisService.updateEmotionAnalysis(analysisId, request);
            return response.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            // SpeechEmotionSaveRequest를 UserEmotionAnalysisRequest로 변환
            UserEmotionAnalysisRequest analysisRequest = new UserEmotionAnalysisRequest();
            analysisRequest.setConversationMessageId(request.getConversationMessageId());
            analysisRequest.setSpeechEmotionData(request.getSpeechEmotionData());
            analysisRequest.setCombinedEmotion(request.getCombinedEmotion());
            analysisRequest.setCombinedConfidence(request.getCombinedConfidence());
            
            UserEmotionAnalysisResponse response = userEmotionAnalysisService.saveEmotionAnalysis(analysisRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}
