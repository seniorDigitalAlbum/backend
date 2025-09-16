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
        
        // 표정 감정만 저장 (통합 감정은 별도 API에서 처리)
        analysis.setCombinedEmotion(request.getFacialEmotionData().getFinalEmotion());
        analysis.setCombinedConfidence(request.getFacialEmotionData().getAverageConfidence());
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
        
        // 말 감정만 저장 (통합 감정은 별도 API에서 처리)
        analysis.setCombinedEmotion(request.getEmotion());
        analysis.setCombinedConfidence(request.getConfidence());
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
    
    
}
