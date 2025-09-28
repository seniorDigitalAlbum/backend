package com.chimaenono.dearmind.conversation;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;
import com.chimaenono.dearmind.conversationMessage.ConversationMessage.SenderType;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;
import com.chimaenono.dearmind.question.Question;
import com.chimaenono.dearmind.question.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Data", description = "테스트용 데이터 생성 API")
public class TestDataController {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @PostMapping("/conversation")
    @Operation(summary = "테스트용 대화 데이터 생성", description = "일기 테스트를 위한 샘플 대화 데이터를 생성합니다")
    public ResponseEntity<String> createTestConversation() {
        try {
            // 1. 질문 데이터 확인/생성
            Question question = questionRepository.findById(1L).orElse(null);
            if (question == null) {
                question = new Question();
                question.setId(1L);
                question.setContent("어린 시절 추억에 대해 이야기해주세요");
                questionRepository.save(question);
            }
            
            // 2. 대화 세션 생성
            Conversation conversation = new Conversation();
            conversation.setUserId(1L);
            conversation.setQuestionId(1L);
            conversation.setCameraSessionId("camera_test_123");
            conversation.setMicrophoneSessionId("microphone_test_456");
            conversation.setStatus(Conversation.ConversationStatus.ACTIVE);
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setProcessingStatus(Conversation.ProcessingStatus.READY);
            
            conversation = conversationRepository.save(conversation);
            Long conversationId = conversation.getId();
            
            // 3. 대화 메시지 생성
            createTestMessages(conversationId);
            
            // 4. 감정 분석 데이터 생성
            createTestEmotions(conversationId);
            
            return ResponseEntity.ok("테스트 대화 데이터가 생성되었습니다. Conversation ID: " + conversationId);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("테스트 데이터 생성 중 오류: " + e.getMessage());
        }
    }
    
    private void createTestMessages(Long conversationId) {
        // 사용자 메시지들
        String[] userMessages = {
            "안녕하세요. 오늘은 어린 시절 추억에 대해 이야기하고 싶어요.",
            "어릴 때 할머니 댁에서 그네를 타고 놀던 기억이 있어요.",
            "그네를 타면서 하늘을 보니 정말 기분이 좋았어요.",
            "할머니가 맛있는 과자를 만들어 주셨던 것도 기억나요.",
            "그때가 정말 행복했던 시절이었어요."
        };
        
        // AI 메시지들
        String[] aiMessages = {
            "안녕하세요! 어린 시절 추억에 대해 이야기해주셔서 감사해요. 어떤 추억이 가장 기억에 남으시나요?",
            "그네를 타고 놀던 기억이군요! 그네를 타면서 하늘을 보는 순간이 정말 특별했을 것 같아요.",
            "하늘을 보면서 느꼈던 그 기분을 더 자세히 말씀해주실 수 있나요?",
            "할머니가 만들어주신 과자도 정말 맛있었을 것 같아요. 어떤 과자였나요?",
            "그런 행복한 추억들을 가지고 계시니 정말 좋으시겠어요. 그때의 감정을 다시 느껴보시는 것 같아요."
        };
        
        // 메시지 생성
        for (int i = 0; i < userMessages.length; i++) {
            // 사용자 메시지
            ConversationMessage userMessage = new ConversationMessage();
            userMessage.setConversationId(conversationId);
            userMessage.setContent(userMessages[i]);
            userMessage.setSenderType(SenderType.USER);
            userMessage.setTimestamp(LocalDateTime.now().plusMinutes(i * 2));
            conversationMessageRepository.save(userMessage);
            
            // AI 메시지
            ConversationMessage aiMessage = new ConversationMessage();
            aiMessage.setConversationId(conversationId);
            aiMessage.setContent(aiMessages[i]);
            aiMessage.setSenderType(SenderType.AI);
            aiMessage.setTimestamp(LocalDateTime.now().plusMinutes(i * 2 + 1));
            conversationMessageRepository.save(aiMessage);
        }
    }
    
    private void createTestEmotions(Long conversationId) {
        // 대화 메시지 조회
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        
        // 사용자 메시지에만 감정 분석 데이터 생성
        for (ConversationMessage message : messages) {
            if (message.getSenderType() == SenderType.USER) {
                UserEmotionAnalysis emotion = new UserEmotionAnalysis();
                emotion.setConversationMessage(message);
                
                // JSON 형태로 감정 데이터 설정
                emotion.setFacialEmotion("{\"final_emotion\":\"기쁨\",\"confidence\":0.85,\"emotion_count\":{\"기쁨\":3,\"중립\":1},\"total_captures\":4}");
                emotion.setSpeechEmotion("{\"emotion\":\"기쁨\",\"confidence\":0.78}");
                emotion.setCombinedEmotion("기쁨");
                emotion.setCombinedConfidence(0.82);
                emotion.setAnalysisTimestamp(LocalDateTime.now());
                
                userEmotionAnalysisRepository.save(emotion);
            }
        }
    }
    
    @GetMapping("/conversations")
    @Operation(summary = "테스트 대화 목록 조회", description = "생성된 테스트 대화 목록을 조회합니다")
    public ResponseEntity<List<Conversation>> getTestConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return ResponseEntity.ok(conversations);
    }
}
