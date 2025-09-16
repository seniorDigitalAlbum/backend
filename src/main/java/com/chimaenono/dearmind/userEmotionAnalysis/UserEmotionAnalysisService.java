package com.chimaenono.dearmind.userEmotionAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Tag(name = "UserEmotionAnalysis", description = "사용자 감정 분석 관리 서비스")
public class UserEmotionAnalysisService {
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Operation(summary = "표정 감정 분석 결과 저장", description = "표정 감정 분석 결과를 저장합니다")
    @Transactional
    public UserEmotionAnalysisResponse saveFacialEmotionAnalysis(FacialEmotionSaveRequest request) {
        // 대화 메시지 조회
        Optional<ConversationMessage> messageOpt = conversationMessageRepository.findById(request.getConversationMessageId());
        if (!messageOpt.isPresent()) {
            throw new RuntimeException("대화 메시지를 찾을 수 없습니다: " + request.getConversationMessageId());
        }
        
        ConversationMessage message = messageOpt.get();
        
        // UserEmotionAnalysis 엔티티 생성 또는 업데이트
        Optional<UserEmotionAnalysis> existingAnalysis = userEmotionAnalysisRepository.findByConversationMessageId(request.getConversationMessageId());
        UserEmotionAnalysis analysis;
        
        if (existingAnalysis.isPresent()) {
            // 기존 레코드 업데이트
            analysis = existingAnalysis.get();
        } else {
            // 새 레코드 생성
            analysis = new UserEmotionAnalysis();
            analysis.setConversationMessage(message);
        }
        
        // 표정 감정 데이터 JSON 변환
        try {
            String facialEmotionJson = objectMapper.writeValueAsString(request.getFacialEmotionData());
            analysis.setFacialEmotion(facialEmotionJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("표정 감정 데이터 JSON 변환 실패: " + e.getMessage());
        }
        
        // 표정 감정 저장 시 combinedEmotion 처리
        if (analysis.getSpeechEmotion() != null) {
            // 말 감정도 있는 경우: 통합 로직 적용
            analysis = applyCombinedEmotionLogic(analysis);
        } else {
            // 표정 감정만 있는 경우: 표정 감정을 combined로 설정
            analysis.setCombinedEmotion(request.getFacialEmotionData().getFinalEmotion());
            analysis.setCombinedConfidence(request.getFacialEmotionData().getAverageConfidence());
        }
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        
        // 데이터베이스에 저장
        UserEmotionAnalysis savedAnalysis = userEmotionAnalysisRepository.save(analysis);
        
        // Response DTO로 변환하여 반환
        return UserEmotionAnalysisResponse.from(savedAnalysis);
    }
    
    @Operation(summary = "말 감정 분석 결과 저장", description = "말 감정 분석 결과를 저장합니다")
    @Transactional
    public UserEmotionAnalysisResponse saveSpeechEmotionAnalysis(SpeechEmotionSaveRequest request) {
        // 대화 메시지 조회
        Optional<ConversationMessage> messageOpt = conversationMessageRepository.findById(request.getConversationMessageId());
        if (!messageOpt.isPresent()) {
            throw new RuntimeException("대화 메시지를 찾을 수 없습니다: " + request.getConversationMessageId());
        }
        
        ConversationMessage message = messageOpt.get();
        
        // UserEmotionAnalysis 엔티티 생성 또는 업데이트
        Optional<UserEmotionAnalysis> existingAnalysis = userEmotionAnalysisRepository.findByConversationMessageId(request.getConversationMessageId());
        UserEmotionAnalysis analysis;
        
        if (existingAnalysis.isPresent()) {
            // 기존 레코드 업데이트
            analysis = existingAnalysis.get();
        } else {
            // 새 레코드 생성
            analysis = new UserEmotionAnalysis();
            analysis.setConversationMessage(message);
        }
        
        // 말 감정 데이터 설정
        analysis.setSpeechEmotion(request.getSpeechEmotionData());
        
        // 말 감정 저장 시 combinedEmotion 처리
        if (analysis.getFacialEmotion() != null) {
            // 표정 감정도 있는 경우: 통합 로직 적용
            analysis = applyCombinedEmotionLogic(analysis);
        } else {
            // 말 감정만 있는 경우: 말 감정을 combined로 설정
            analysis.setCombinedEmotion(request.getEmotion());
            analysis.setCombinedConfidence(request.getConfidence());
        }
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        
        // 데이터베이스에 저장
        UserEmotionAnalysis savedAnalysis = userEmotionAnalysisRepository.save(analysis);
        
        // Response DTO로 변환하여 반환
        return UserEmotionAnalysisResponse.from(savedAnalysis);
    }
    
    @Operation(summary = "특정 메시지의 감정 분석 결과 조회", description = "대화 메시지 ID로 감정 분석 결과를 조회합니다")
    public Optional<UserEmotionAnalysisResponse> getEmotionAnalysisByMessageId(Long conversationMessageId) {
        Optional<UserEmotionAnalysis> analysis = userEmotionAnalysisRepository.findByConversationMessageId(conversationMessageId);
        return analysis.map(UserEmotionAnalysisResponse::from);
    }
    
    @Operation(summary = "대화 세션의 모든 감정 분석 결과 조회", description = "특정 대화 세션의 모든 감정 분석 결과를 조회합니다")
    public List<UserEmotionAnalysisResponse> getEmotionAnalysesByConversationId(Long conversationId) {
        List<UserEmotionAnalysis> analyses = userEmotionAnalysisRepository.findByConversationMessageConversationIdOrderByAnalysisTimestampAsc(conversationId);
        return analyses.stream()
            .map(UserEmotionAnalysisResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "특정 감정으로 필터링된 감정 분석 결과 조회", description = "통합 감정으로 필터링된 감정 분석 결과를 조회합니다")
    public List<UserEmotionAnalysisResponse> getEmotionAnalysesByEmotion(String emotion) {
        List<UserEmotionAnalysis> analyses = userEmotionAnalysisRepository.findByCombinedEmotion(emotion);
        return analyses.stream()
            .map(UserEmotionAnalysisResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "신뢰도 범위로 필터링된 감정 분석 결과 조회", description = "최소 신뢰도 이상의 감정 분석 결과를 조회합니다")
    public List<UserEmotionAnalysisResponse> getEmotionAnalysesByMinConfidence(Double minConfidence) {
        List<UserEmotionAnalysis> analyses = userEmotionAnalysisRepository.findByCombinedConfidenceGreaterThanEqual(minConfidence);
        return analyses.stream()
            .map(UserEmotionAnalysisResponse::from)
            .collect(Collectors.toList());
    }
    
    @Operation(summary = "감정 분석 결과 삭제", description = "특정 감정 분석 결과를 삭제합니다")
    @Transactional
    public boolean deleteEmotionAnalysis(Long analysisId) {
        if (userEmotionAnalysisRepository.existsById(analysisId)) {
            userEmotionAnalysisRepository.deleteById(analysisId);
            return true;
        }
        return false;
    }
    
    /**
     * 표정 감정과 말 감정을 통합하여 최종 감정을 계산하는 로직
     */
    private UserEmotionAnalysis applyCombinedEmotionLogic(UserEmotionAnalysis analysis) {
        try {
            // 표정 감정 데이터 파싱 - 두 형식 모두 지원
            @SuppressWarnings("unchecked")
            Map<String, Object> facialDataMap = objectMapper.readValue(analysis.getFacialEmotion(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> speechData = objectMapper.readValue(analysis.getSpeechEmotion(), Map.class);
            
            // 표정 감정 데이터 추출 (구/신 형식 모두 지원)
            String facialEmotionEnglish = extractFacialEmotion(facialDataMap);
            Double facialConfidence = extractFacialConfidence(facialDataMap);
            
            // 감정 매핑 (영어 ↔ 한국어)
            Map<String, String> emotionMapping = Map.of(
                "joy", "기쁨",
                "embarrassed", "당황", 
                "anger", "분노",
                "anxious", "불안",
                "hurt", "상처",
                "sad", "슬픔",
                "neutral", "중립"
            );
            
            // 감정 변환
            String facialEmotionKorean = emotionMapping.get(facialEmotionEnglish);
            String speechEmotionKorean = (String) speechData.get("predicted_label");
            
            // Neutral 감정은 계산 대상에서 제외
            boolean isFacialNeutral = "neutral".equals(facialEmotionEnglish) || 
                                     facialConfidence == 0.0;
            boolean isSpeechNeutral = "중립".equals(speechEmotionKorean) || 
                                     "neutral".equals(speechEmotionKorean) ||
                                     speechEmotionKorean == null;
            
            // 둘 다 neutral인 경우
            if (isFacialNeutral && isSpeechNeutral) {
                analysis.setCombinedEmotion("neutral");
                analysis.setCombinedConfidence(0.0);
                return analysis;
            }
            
            // 하나만 neutral인 경우: non-neutral 것을 사용
            if (isFacialNeutral && !isSpeechNeutral) {
                // 표정이 neutral이면 말 감정 사용
                String speechEmotionEnglish = emotionMapping.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(speechEmotionKorean))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("neutral");
                
                Double speechConfidence = ((Number) speechData.get("confidence")).doubleValue();
                analysis.setCombinedEmotion(speechEmotionEnglish);
                analysis.setCombinedConfidence(speechConfidence);
                return analysis;
            }
            
            if (!isFacialNeutral && isSpeechNeutral) {
                // 말이 neutral이면 표정 감정 사용
                analysis.setCombinedEmotion(facialEmotionEnglish);
                analysis.setCombinedConfidence(facialConfidence);
                return analysis;
            }
            
            // 감정 일치 여부 확인
            boolean isSameEmotion = facialEmotionKorean != null && facialEmotionKorean.equals(speechEmotionKorean);
            
            if (isSameEmotion) {
                // 같은 감정: 평균 신뢰도
                Double speechConfidence = ((Number) speechData.get("confidence")).doubleValue();
                double averageConfidence = (facialConfidence + speechConfidence) / 2;
                
                analysis.setCombinedEmotion(facialEmotionEnglish);
                analysis.setCombinedConfidence(averageConfidence);
            } else {
                // 다른 감정: 신뢰도가 높은 것 선택
                Double speechConfidence = ((Number) speechData.get("confidence")).doubleValue();
                
                if (facialConfidence > speechConfidence) {
                    analysis.setCombinedEmotion(facialEmotionEnglish);
                    analysis.setCombinedConfidence(facialConfidence);
                } else {
                    // 한국어 감정을 영어로 변환
                    String speechEmotionEnglish = emotionMapping.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(speechEmotionKorean))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("neutral");
                    
                    analysis.setCombinedEmotion(speechEmotionEnglish);
                    analysis.setCombinedConfidence(speechConfidence);
                }
            }
            
        } catch (JsonProcessingException e) {
            // JSON 파싱 실패 시 기본값 설정
            analysis.setCombinedEmotion("neutral");
            analysis.setCombinedConfidence(0.0);
        }
        
        return analysis;
    }
    
    /**
     * 표정 감정 데이터에서 감정을 추출 (구/신 형식 모두 지원)
     */
    private String extractFacialEmotion(Map<String, Object> facialDataMap) {
        // 신 형식: finalEmotion
        if (facialDataMap.containsKey("finalEmotion")) {
            return (String) facialDataMap.get("finalEmotion");
        }
        
        // 구 형식: final_emotion
        if (facialDataMap.containsKey("final_emotion")) {
            String emotion = (String) facialDataMap.get("final_emotion");
            // 한국어를 영어로 변환
            return convertKoreanToEnglish(emotion);
        }
        
        return "neutral";
    }
    
    /**
     * 표정 감정 데이터에서 신뢰도를 추출 (구/신 형식 모두 지원)
     */
    private Double extractFacialConfidence(Map<String, Object> facialDataMap) {
        // 신 형식: averageConfidence
        if (facialDataMap.containsKey("averageConfidence")) {
            return ((Number) facialDataMap.get("averageConfidence")).doubleValue();
        }
        
        // 구 형식: confidence
        if (facialDataMap.containsKey("confidence")) {
            return ((Number) facialDataMap.get("confidence")).doubleValue();
        }
        
        return 0.0;
    }
    
    /**
     * 한국어 감정을 영어로 변환
     */
    private String convertKoreanToEnglish(String koreanEmotion) {
        Map<String, String> koreanToEnglish = Map.of(
            "기쁨", "joy",
            "당황", "embarrassed", 
            "분노", "anger",
            "불안", "anxious",
            "상처", "hurt",
            "슬픔", "sad",
            "중립", "neutral"
        );
        
        return koreanToEnglish.getOrDefault(koreanEmotion, "neutral");
    }
    
}
