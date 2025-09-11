package com.chimaenono.dearmind.userEmotionAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.Optional;

@Service
@Tag(name = "CombineEmotion", description = "통합 감정 계산 서비스")
public class CombineEmotionService {
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 감정 매핑 테이블 (영어 ↔ 한국어)
    private final Map<String, String> emotionMapping = Map.of(
        "joy", "기쁨",
        "sadness", "슬픔", 
        "anger", "화남",
        "fear", "두려움",
        "surprise", "놀람",
        "disgust", "혐오",
        "neutral", "중립"
    );
    
    @Operation(summary = "통합 감정 계산 및 저장", description = "표정 감정과 말 감정을 통합하여 최종 감정을 계산하고 저장합니다")
    @Transactional
    public UserEmotionAnalysisResponse combineEmotions(Long conversationMessageId) {
        // 1. 기존 감정 분석 데이터 조회
        Optional<UserEmotionAnalysis> analysisOpt = userEmotionAnalysisRepository.findByConversationMessageId(conversationMessageId);
        if (!analysisOpt.isPresent()) {
            throw new RuntimeException("감정 분석 데이터를 찾을 수 없습니다: " + conversationMessageId);
        }
        
        UserEmotionAnalysis analysis = analysisOpt.get();
        
        // 2. 표정 감정과 말 감정 데이터 검증
        if (analysis.getFacialEmotion() == null || analysis.getSpeechEmotion() == null) {
            throw new RuntimeException("표정 감정 또는 말 감정 데이터가 없습니다. 먼저 각각의 감정 분석을 완료해주세요.");
        }
        
        // 3. 통합 감정 계산
        CombinedEmotionResult combinedResult = calculateCombinedEmotion(
            analysis.getFacialEmotion(), 
            analysis.getSpeechEmotion()
        );
        
        // 4. 데이터베이스 업데이트
        analysis.setCombinedEmotion(combinedResult.getEmotion());
        analysis.setCombinedConfidence(combinedResult.getConfidence());
        
        UserEmotionAnalysis updatedAnalysis = userEmotionAnalysisRepository.save(analysis);
        
        // 5. 응답 반환
        return UserEmotionAnalysisResponse.from(updatedAnalysis);
    }
    
    private CombinedEmotionResult calculateCombinedEmotion(String facialEmotionJson, String speechEmotionJson) {
        try {
            // 1. JSON 파싱
            FacialEmotionData facialData = objectMapper.readValue(facialEmotionJson, FacialEmotionData.class);
            Map<String, Object> speechData = objectMapper.readValue(speechEmotionJson, Map.class);
            
            // 2. 감정 매핑 (영어 → 한국어)
            String facialEmotionKorean = emotionMapping.get(facialData.getFinalEmotion());
            String speechEmotionKorean = (String) speechData.get("emotion");
            
            if (facialEmotionKorean == null || speechEmotionKorean == null) {
                throw new RuntimeException("알 수 없는 감정 값입니다. facial: " + facialData.getFinalEmotion() + ", speech: " + speechEmotionKorean);
            }
            
            // 3. 감정 일치 여부 확인
            boolean isSameEmotion = facialEmotionKorean.equals(speechEmotionKorean);
            
            if (isSameEmotion) {
                // 같은 감정: 평균 신뢰도
                Double speechConfidence = ((Number) speechData.get("confidence")).doubleValue();
                double averageConfidence = (facialData.getAverageConfidence() + speechConfidence) / 2;
                
                return new CombinedEmotionResult(
                    facialData.getFinalEmotion(), // 영어 감정 사용
                    averageConfidence
                );
            } else {
                // 다른 감정: 신뢰도가 높은 것 선택
                Double speechConfidence = ((Number) speechData.get("confidence")).doubleValue();
                
                if (facialData.getAverageConfidence() > speechConfidence) {
                    return new CombinedEmotionResult(
                        facialData.getFinalEmotion(),
                        facialData.getAverageConfidence()
                    );
                } else {
                    // 한국어 감정을 영어로 변환
                    String speechEmotionEnglish = emotionMapping.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(speechEmotionKorean))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("neutral");
                    
                    return new CombinedEmotionResult(
                        speechEmotionEnglish,
                        speechConfidence
                    );
                }
            }
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("감정 데이터 JSON 파싱 실패: " + e.getMessage());
        }
    }
    
    // 통합 감정 결과를 담는 내부 클래스
    private static class CombinedEmotionResult {
        private final String emotion;
        private final Double confidence;
        
        public CombinedEmotionResult(String emotion, Double confidence) {
            this.emotion = emotion;
            this.confidence = confidence;
        }
        
        public String getEmotion() { return emotion; }
        public Double getConfidence() { return confidence; }
    }
}
