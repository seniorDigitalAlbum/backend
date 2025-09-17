package com.chimaenono.dearmind.diary;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;
import com.chimaenono.dearmind.conversation.Conversation;
import com.chimaenono.dearmind.conversation.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiaryPlanService {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    /**
     * EmotionFlow와 Summary를 기반으로 DiaryPlan을 생성합니다.
     */
    public DiaryPlan buildDiaryPlan(Long conversationId) {
        try {
            // 1. Conversation 조회 (emotionFlow, summary 포함)
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
            
            // 2. 대화 메시지 조회 (USER 메시지만)
            List<ConversationMessage> allMessages = conversationMessageRepository
                    .findByConversationIdOrderByTimestampAsc(conversationId);
            List<ConversationMessage> userMessages = allMessages.stream()
                    .filter(msg -> msg.getSenderType() == ConversationMessage.SenderType.USER)
                    .collect(Collectors.toList());
            
            // 3. EmotionFlow 파싱
            EmotionFlow flow = parseEmotionFlow(conversation.getEmotionFlow());
            
            // 4. Summary 파싱
            Summary summary = parseSummary(conversation.getSummary());
            
            // 5. DiaryPlan 생성
            return buildDiaryPlan(flow, summary, userMessages);
            
        } catch (Exception e) {
            log.error("DiaryPlan 생성 중 오류 발생: conversationId={}", conversationId, e);
            throw new RuntimeException("DiaryPlan 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 실제 DiaryPlan 생성 로직
     */
    private DiaryPlan buildDiaryPlan(EmotionFlow flow, Summary summary, List<ConversationMessage> userMessages) {
        DiaryPlan dp = new DiaryPlan();
        
        // 1) paragraphPlan
        int turning = flow.getMetrics().getPeakArousalTurn();
        int tSeg = findSeg(flow.getSegments(), turning);
        int last = flow.getSegments().size() - 1;
        dp.setParagraphPlan(Map.of("para1", 0, "para2", tSeg, "para3", last));
        
        // 2) styleHints
        String firstDominant = flow.getSegments().get(0).getDominant();
        String lastDominant = flow.getSegments().get(last).getDominant();
        dp.setStyleHints(styleFrom(flow.getMetrics().getPattern(), firstDominant, lastDominant));
        
        // 3) 세그먼트 패키징
        for (int i = 0; i < flow.getSegments().size(); i++) {
            EmotionFlow.Segment segment = flow.getSegments().get(i);
            
            // 해당 세그먼트 범위의 사용자 메시지 필터링
            List<ConversationMessage> segmentMessages = userMessages.stream()
                    .filter(msg -> {
                        int turnIndex = userMessages.indexOf(msg); // 사용자 메시지 내에서의 순서
                        return turnIndex >= segment.getStartTurn() && turnIndex <= segment.getEndTurn();
                    })
                    .collect(Collectors.toList());
            
            String quote = pickQuote(segmentMessages, segment.getDominant());
            Map<String, Object> anchors = pickAnchors(summary, segmentMessages);
            String micro = ruleBasedMicroSummary(segmentMessages, anchors);
            
            dp.getSegments().add(new DiaryPlan.DPSeg(i, segment.getStartTurn(), segment.getEndTurn(), 
                    segment.getDominant(), micro, anchors, quote));
        }
        
        // 4) opening/turningPoint/closing
        dp.setFlowPattern(flow.getMetrics().getPattern());
        dp.setTurningPoint(Map.of("turn", turning, "dominant", flow.getSegments().get(tSeg).getDominant()));
        dp.setOpening(Map.of("dominant", firstDominant, "valenceMean", flow.getSegments().get(0).getValenceMean()));
        dp.setClosing(Map.of("dominant", lastDominant, "valenceMean", flow.getSegments().get(last).getValenceMean()));
        
        return dp;
    }
    
    /**
     * 특정 turn이 속한 세그먼트 인덱스를 찾습니다.
     */
    private int findSeg(List<EmotionFlow.Segment> segments, int turning) {
        for (int i = 0; i < segments.size(); i++) {
            EmotionFlow.Segment seg = segments.get(i);
            if (turning >= seg.getStartTurn() && turning <= seg.getEndTurn()) {
                return i;
            }
        }
        return 0; // 기본값
    }
    
    /**
     * 감정 패턴과 시작/끝 감정을 기반으로 스타일 힌트를 생성합니다.
     */
    private Map<String, String> styleFrom(String pattern, String firstDominant, String lastDominant) {
        Map<String, String> hints = new HashMap<>();
        
        hints.put("pattern", pattern);
        hints.put("tone", determineTone(pattern, firstDominant, lastDominant));
        hints.put("structure", determineStructure(pattern));
        
        return hints;
    }
    
    private String determineTone(String pattern, String first, String last) {
        switch (pattern) {
            case "U-shape":
                return "희망적";
            case "상승형":
                return "긍정적";
            case "하강형":
                return "성찰적";
            case "급반전형":
                return "역동적";
            default:
                return "차분한";
        }
    }
    
    private String determineStructure(String pattern) {
        switch (pattern) {
            case "U-shape":
                return "기승전결";
            case "상승형":
                return "점진적발전";
            case "하강형":
                return "회상중심";
            default:
                return "시간순";
        }
    }
    
    /**
     * 세그먼트에서 대표 인용문을 선택합니다.
     */
    private String pickQuote(List<ConversationMessage> messages, String dominant) {
        // 30자 이하의 메시지 중에서 가장 의미있는 것을 선택
        return messages.stream()
                .filter(msg -> msg.getContent().length() <= 30)
                .filter(msg -> msg.getContent().length() > 5) // 너무 짧은 것 제외
                .findFirst()
                .map(ConversationMessage::getContent)
                .orElse("");
    }
    
    /**
     * 요약의 anchors와 메시지를 매칭하여 관련 anchors를 추출합니다.
     */
    private Map<String, Object> pickAnchors(Summary summary, List<ConversationMessage> messages) {
        Map<String, Object> anchors = new HashMap<>();
        
        if (summary != null && summary.getAnchors() != null) {
            // 메시지 내용과 매칭되는 anchors 찾기
            String messageText = messages.stream()
                    .map(ConversationMessage::getContent)
                    .collect(Collectors.joining(" "));
            
            // 간단한 키워드 매칭 (실제로는 더 정교한 매칭 로직 필요)
            if (summary.getAnchors().getPeople() != null) {
                List<String> matchedPeople = summary.getAnchors().getPeople().stream()
                        .filter(person -> messageText.contains(person))
                        .collect(Collectors.toList());
                if (!matchedPeople.isEmpty()) {
                    anchors.put("people", matchedPeople);
                }
            }
            
            if (summary.getAnchors().getPlace() != null) {
                List<String> matchedPlaces = summary.getAnchors().getPlace().stream()
                        .filter(place -> messageText.contains(place))
                        .collect(Collectors.toList());
                if (!matchedPlaces.isEmpty()) {
                    anchors.put("place", matchedPlaces);
                }
            }
            
            if (summary.getAnchors().getEra() != null && messageText.contains(summary.getAnchors().getEra())) {
                anchors.put("era", summary.getAnchors().getEra());
            }
            
            if (summary.getAnchors().getObjects() != null) {
                List<String> matchedObjects = summary.getAnchors().getObjects().stream()
                        .filter(object -> messageText.contains(object))
                        .collect(Collectors.toList());
                if (!matchedObjects.isEmpty()) {
                    anchors.put("objects", matchedObjects);
                }
            }
        }
        
        return anchors;
    }
    
    /**
     * 규칙 기반으로 마이크로 요약을 생성합니다.
     */
    private String ruleBasedMicroSummary(List<ConversationMessage> messages, Map<String, Object> anchors) {
        if (messages.isEmpty()) {
            return "";
        }
        
        // 첫 번째와 마지막 메시지를 기반으로 간단한 요약 생성
        String firstMessage = messages.get(0).getContent();
        String summary = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
        
        // anchors 정보가 있으면 추가
        if (anchors.containsKey("people")) {
            summary += " (" + anchors.get("people") + "와 함께)";
        }
        
        return summary;
    }
    
    /**
     * JSON 문자열을 EmotionFlow 객체로 파싱합니다.
     */
    private EmotionFlow parseEmotionFlow(String emotionFlowJson) {
        try {
            if (emotionFlowJson == null || emotionFlowJson.trim().isEmpty()) {
                return createEmptyEmotionFlow();
            }
            return objectMapper.readValue(emotionFlowJson, EmotionFlow.class);
        } catch (Exception e) {
            log.warn("EmotionFlow 파싱 실패, 기본값 사용: {}", e.getMessage());
            return createEmptyEmotionFlow();
        }
    }
    
    /**
     * JSON 문자열을 Summary 객체로 파싱합니다.
     */
    private Summary parseSummary(String summaryJson) {
        try {
            if (summaryJson == null || summaryJson.trim().isEmpty()) {
                return new Summary();
            }
            return objectMapper.readValue(summaryJson, Summary.class);
        } catch (Exception e) {
            log.warn("Summary 파싱 실패, 기본값 사용: {}", e.getMessage());
            return new Summary();
        }
    }
    
    /**
     * 빈 EmotionFlow 객체를 생성합니다.
     */
    private EmotionFlow createEmptyEmotionFlow() {
        EmotionFlow flow = new EmotionFlow();
        EmotionFlow.Metrics metrics = new EmotionFlow.Metrics();
        metrics.setPattern("안정형");
        metrics.setPeakArousalTurn(0);
        flow.setMetrics(metrics);
        
        EmotionFlow.Segment segment = new EmotionFlow.Segment();
        segment.setStartTurn(0);
        segment.setEndTurn(0);
        segment.setDominant("중립");
        segment.setValenceMean(0.0);
        flow.setSegments(List.of(segment));
        
        return flow;
    }
}
