package com.chimaenono.dearmind.userEmotionAnalysis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emotion-analysis/test")
@RequiredArgsConstructor
@Tag(name = "Emotion Analysis Test", description = "감정 분석 테스트 API")
public class CombineEmotionTestController {

    private final CombineEmotionService combineEmotionService;

    @PostMapping("/combine-calculation")
    @Operation(summary = "감정 통합 계산 테스트", description = "facialEmotion과 speechEmotion을 입력받아 combinedEmotion 계산 과정을 상세히 보여줍니다")
    public ResponseEntity<Map<String, Object>> testCombineEmotionCalculation(
            @RequestBody CombineEmotionTestRequest request) {
        
        try {
            Map<String, Object> result = combineEmotionService.testCombineEmotionCalculation(
                request.getFacialEmotionJson(), 
                request.getSpeechEmotionJson()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "감정 통합 계산 테스트 실패",
                "message", e.getMessage()
            ));
        }
    }

    // 테스트 요청 DTO
    public static class CombineEmotionTestRequest {
        private String facialEmotionJson;
        private String speechEmotionJson;

        public String getFacialEmotionJson() {
            return facialEmotionJson;
        }

        public void setFacialEmotionJson(String facialEmotionJson) {
            this.facialEmotionJson = facialEmotionJson;
        }

        public String getSpeechEmotionJson() {
            return speechEmotionJson;
        }

        public void setSpeechEmotionJson(String speechEmotionJson) {
            this.speechEmotionJson = speechEmotionJson;
        }
    }
}
