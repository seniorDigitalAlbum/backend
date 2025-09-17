package com.chimaenono.dearmind.diary;

import com.chimaenono.dearmind.gpt.GPTService;
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

@Slf4j
@RestController
@RequestMapping("/api/diary/test")
@Tag(name = "Diary Test", description = "일기 생성 테스트 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class DiaryTestController {

    @Autowired
    private GPTService gptService;

    @Operation(summary = "일기 생성 테스트", 
               description = "특정 conversationId에 대해 일기를 생성하고 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "일기 생성 성공"),
        @ApiResponse(responseCode = "404", description = "대화 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/generate/{conversationId}")
    public ResponseEntity<?> generateDiary(
            @Parameter(description = "대화 세션 ID", example = "1001")
            @PathVariable Long conversationId) {
        
        try {
            log.info("일기 생성 테스트 시작: conversationId={}", conversationId);
            
            // 요약이 있는지 확인하고, 없으면 기본값 사용
            String summary = "대화 요약이 없어 기본값을 사용합니다.";
            
            // 일기 생성 및 저장
            String diary = gptService.generateAndSaveDiary(conversationId, summary);
            
            log.info("일기 생성 완료: conversationId={}, 일기 길이={}", conversationId, diary.length());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "일기가 성공적으로 생성되고 저장되었습니다.",
                "conversationId", conversationId,
                "diary", diary,
                "diaryLength", diary.length()
            ));
            
        } catch (Exception e) {
            log.error("일기 생성 중 오류 발생: conversationId={}", conversationId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "일기 생성 중 오류가 발생했습니다.",
                "message", e.getMessage(),
                "conversationId", conversationId
            ));
        }
    }

    @Operation(summary = "서비스 상태 확인", description = "Diary 테스트 서비스 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Diary Test service is running");
    }
}
