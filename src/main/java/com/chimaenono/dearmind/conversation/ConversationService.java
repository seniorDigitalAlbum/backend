package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;
import com.chimaenono.dearmind.question.Question;
import com.chimaenono.dearmind.question.QuestionRepository;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysis;
import com.chimaenono.dearmind.userEmotionAnalysis.UserEmotionAnalysisRepository;
import com.chimaenono.dearmind.camera.CameraService;
import com.chimaenono.dearmind.microphone.MicrophoneService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@Tag(name = "Conversation Service", description = "대화 세션 관리 서비스")
public class ConversationService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private UserEmotionAnalysisRepository userEmotionAnalysisRepository;
    
    @Autowired
    private CameraService cameraService;
    
    @Autowired
    @Lazy
    private MicrophoneService microphoneService;
    
    @Operation(summary = "통합 대화 시작", description = "카메라 세션, 마이크 세션, 대화방을 통합으로 생성합니다")
    public ConversationStartResponse startConversation(ConversationStartRequest request, Long userId) {
        // 입력 검증
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (request.getQuestionId() == null) {
            throw new IllegalArgumentException("질문 ID는 필수입니다.");
        }
        
        // 질문 존재 여부 확인
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문 ID입니다: " + request.getQuestionId()));
        
        try {
            // 1. 카메라 세션 생성
            String cameraSessionId = cameraService.createSession(userId).getSessionId();
            
            // 2. 마이크 세션 생성
            String microphoneSessionId = microphoneService.createSession(userId, "WAV", 44100).getSessionId();
            
            // 3. 대화방 생성
            Conversation conversation = createConversation(
                userId, 
                request.getQuestionId(), 
                cameraSessionId, 
                microphoneSessionId
            );
            
            // 4. 응답 생성
            return ConversationStartResponse.success(
                conversation.getId(),
                cameraSessionId,
                microphoneSessionId,
                conversation.getStatus().toString(),
                question
            );
            
        } catch (Exception e) {
            throw new RuntimeException("대화 시작 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    @Operation(summary = "대화 세션 생성", description = "새로운 대화 세션을 생성합니다")
    public Conversation createConversation(Long userId, Long questionId, String cameraSessionId, String microphoneSessionId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setQuestionId(questionId);
        conversation.setCameraSessionId(cameraSessionId);
        conversation.setMicrophoneSessionId(microphoneSessionId);
        conversation.setStatus(Conversation.ConversationStatus.ACTIVE);
        conversation.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }
    
    @Operation(summary = "대화 세션 조회", description = "ID로 대화 세션을 조회합니다")
    public Optional<Conversation> getConversationById(Long conversationId) {
        return conversationRepository.findById(conversationId);
    }
    
    /**
     * Conversation 저장 (facetHistory, targetAnchor 업데이트용)
     */
    public void saveConversation(Conversation conversation) {
        conversationRepository.save(conversation);
    }
    
    @Operation(summary = "사용자별 대화 세션 조회", description = "사용자의 모든 대화 세션을 최신순으로 조회합니다")
    public List<Conversation> getConversationsByUser(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Operation(summary = "사용자의 활성 대화 세션 조회", description = "사용자의 활성 상태 대화 세션을 조회합니다")
    public Optional<Conversation> getActiveConversationByUser(Long userId) {
        return conversationRepository.findByUserIdAndStatus(userId, Conversation.ConversationStatus.ACTIVE);
    }
    
    @Operation(summary = "질문별 대화 세션 조회", description = "특정 질문에 대한 모든 대화 세션을 조회합니다")
    public List<Conversation> getConversationsByQuestionId(Long questionId) {
        return conversationRepository.findByQuestionIdOrderByCreatedAtDesc(questionId);
    }
    
    @Operation(summary = "카메라 세션으로 대화 조회", description = "카메라 세션 ID로 대화 세션을 조회합니다")
    public Optional<Conversation> getConversationByCameraSessionId(String cameraSessionId) {
        return conversationRepository.findByCameraSessionId(cameraSessionId);
    }
    
    @Operation(summary = "마이크 세션으로 대화 조회", description = "마이크 세션 ID로 대화 세션을 조회합니다")
    public Optional<Conversation> getConversationByMicrophoneSessionId(String microphoneSessionId) {
        return conversationRepository.findByMicrophoneSessionId(microphoneSessionId);
    }
    
    @Operation(summary = "대화 세션 상태 업데이트", description = "대화 세션의 상태를 업데이트합니다")
    public Conversation updateConversationStatus(Long conversationId, Conversation.ConversationStatus status) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        if (optionalConversation.isPresent()) {
            Conversation conversation = optionalConversation.get();
            conversation.setStatus(status);
            if (status == Conversation.ConversationStatus.COMPLETED) {
                conversation.setEndedAt(LocalDateTime.now());
            }
            return conversationRepository.save(conversation);
        }
        return null;
    }
    
    @Operation(summary = "대화 세션 종료", description = "대화 세션을 완료 상태로 변경합니다")
    public Conversation endConversation(Long conversationId) {
        return updateConversationStatus(conversationId, Conversation.ConversationStatus.COMPLETED);
    }
    
    @Operation(summary = "대화 메시지 목록 조회", description = "특정 대화 세션의 모든 메시지를 시간순으로 조회합니다")
    public List<ConversationMessage> getMessagesByConversationId(Long conversationId) {
        return conversationMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }
    
    @Operation(summary = "대화 메시지 저장", description = "새로운 대화 메시지를 저장합니다")
    public ConversationMessage saveMessage(ConversationMessage message) {
        message.setTimestamp(LocalDateTime.now());
        return conversationMessageRepository.save(message);
    }
    
    @Operation(summary = "사용자 메시지 저장", description = "사용자의 메시지를 저장합니다")
    public ConversationMessage saveUserMessage(Long conversationId, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setSenderType(ConversationMessage.SenderType.USER);
        message.setContent(content);
        return saveMessage(message);
    }
    
    @Operation(summary = "AI 메시지 저장", description = "AI의 메시지를 저장합니다")
    public ConversationMessage saveAIMessage(Long conversationId, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setSenderType(ConversationMessage.SenderType.AI);
        message.setContent(content);
        return saveMessage(message);
    }
    
    @Operation(summary = "더미 대화 데이터 생성", description = "테스트용 더미 대화 세션과 메시지를 생성합니다")
    public void createDummyConversations(Long userId) {
        // 기존 대화 데이터가 있으면 생성하지 않음
        if (conversationRepository.countByUserId(userId) > 0) {
            return;
        }
        
        // 더미 대화 세션 생성
        for (int i = 1; i <= 3; i++) {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setQuestionId((long) i);
            conversation.setCameraSessionId("camera_session_" + i);
            conversation.setMicrophoneSessionId("microphone_session_" + i);
            conversation.setStatus(Conversation.ConversationStatus.COMPLETED);
            conversation.setCreatedAt(LocalDateTime.now().minusDays(i));
            conversation.setEndedAt(LocalDateTime.now().minusDays(i).plusMinutes(30));
            
            Conversation savedConversation = conversationRepository.save(conversation);
            
            // 각 대화 세션에 대한 더미 메시지 생성
            createDummyMessages(savedConversation.getId(), i);
        }
    }
    
    private void createDummyMessages(Long conversationId, int sessionNumber) {
        String[][] conversations = {
            {
                "AI", "안녕하세요! 오늘은 어떤 추억에 대해 이야기하고 싶으신가요?",
                "USER", "어린 시절 추억에 대해 이야기하고 싶어요",
                "AI", "어린 시절 추억이라니 좋네요! 어떤 놀이를 가장 좋아하셨나요?",
                "USER", "놀이터에서 친구들과 함께 놀던 기억이 가장 좋았어요",
                "AI", "놀이터에서의 추억이군요! 어떤 놀이를 주로 하셨나요?",
                "USER", "그네를 타고 숨바꼭질을 많이 했어요. 특히 그네를 타면서 하늘을 보는 게 좋았어요",
                "AI", "그네를 타면서 하늘을 보는 순간이 정말 아름다웠겠네요. 그때의 기분을 다시 한번 느껴보시는 건 어떨까요?"
            },
            {
                "AI", "안녕하세요! 오늘은 어떤 이야기를 나누고 싶으신가요?",
                "USER", "가족과 함께한 추억을 이야기하고 싶어요",
                "AI", "가족과의 추억이라니 따뜻하겠네요! 어떤 순간이 가장 기억에 남나요?",
                "USER", "어머니가 해주시던 음식을 먹을 때가 가장 좋았어요",
                "AI", "어머니의 음식이군요! 어떤 음식을 가장 좋아하셨나요?",
                "USER", "김치찌개와 된장찌개를 가장 좋아했어요. 집에 들어오면 김치찌개 냄새가 나면 기분이 좋았어요",
                "AI", "어머니의 김치찌개 냄새가 집에 들어오는 순간을 기다리게 만들었겠네요. 그때의 따뜻함을 다시 한번 느껴보세요."
            },
            {
                "AI", "안녕하세요! 오늘은 어떤 추억을 나누고 싶으신가요?",
                "USER", "첫사랑에 대한 이야기를 하고 싶어요",
                "AI", "첫사랑 이야기라니 로맨틱하네요! 어떤 순간이 가장 기억에 남나요?",
                "USER", "학교 운동장에서 처음 만났을 때가 가장 기억에 남아요",
                "AI", "학교 운동장에서의 첫 만남이군요! 그때 어떤 기분이셨나요?",
                "USER", "심장이 뛰고 손에 땀이 났어요. 말을 하려고 해도 떨려서 제대로 말을 못했어요",
                "AI", "첫사랑을 만났을 때의 설렘과 떨림이 지금도 생생하시겠네요. 그때의 순수한 마음을 다시 한번 느껴보세요."
            }
        };
        
        String[] conversation = conversations[sessionNumber - 1];
        for (int i = 0; i < conversation.length; i += 2) {
            ConversationMessage message = new ConversationMessage();
            message.setConversationId(conversationId);
            message.setSenderType(conversation[i].equals("AI") ? 
                ConversationMessage.SenderType.AI : ConversationMessage.SenderType.USER);
            message.setContent(conversation[i + 1]);
            message.setTimestamp(LocalDateTime.now().minusDays(sessionNumber).plusMinutes(i * 2));
            
            conversationMessageRepository.save(message);
        }
    }
    
    @Operation(summary = "대화 요약 정보 조회", description = "대화 세션의 요약 정보를 조회합니다")
    public ConversationSummaryResponse getConversationSummary(Long conversationId) {
        // 대화 세션 조회
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("대화 세션을 찾을 수 없습니다: " + conversationId);
        }
        
        Conversation conversation = conversationOpt.get();
        
        // 질문 정보 조회
        String questionText = "질문 정보 없음";
        Optional<Question> questionOpt = questionRepository.findById(conversation.getQuestionId());
        if (questionOpt.isPresent()) {
            questionText = questionOpt.get().getContent();
        }
        
        // 메시지 통계 계산
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        int totalMessageCount = messages.size();
        int userMessageCount = (int) messages.stream()
            .filter(msg -> msg.getSenderType() == ConversationMessage.SenderType.USER)
            .count();
        int aiMessageCount = totalMessageCount - userMessageCount;
        
        // 대화 지속 시간 계산
        Long duration = null;
        if (conversation.getEndedAt() != null) {
            duration = ChronoUnit.SECONDS.between(conversation.getCreatedAt(), conversation.getEndedAt());
        }
        
        // 감정 분석 요약 생성
        ConversationSummaryResponse.EmotionSummary emotionSummary = createEmotionSummary(conversationId);
        
        // 응답 객체 생성
        ConversationSummaryResponse response = new ConversationSummaryResponse();
        response.setConversationId(conversationId);
        response.setQuestion(questionText);
        response.setStartTime(conversation.getCreatedAt());
        response.setEndTime(conversation.getEndedAt());
        response.setDuration(duration);
        response.setStatus(conversation.getStatus().toString());
        response.setTotalMessageCount(totalMessageCount);
        response.setUserMessageCount(userMessageCount);
        response.setAiMessageCount(aiMessageCount);
        response.setEmotionSummary(emotionSummary);
        
        return response;
    }
    
    private ConversationSummaryResponse.EmotionSummary createEmotionSummary(Long conversationId) {
        // 해당 대화의 모든 감정 분석 결과 조회
        List<UserEmotionAnalysis> emotionAnalyses = userEmotionAnalysisRepository.findByConversationMessageConversationIdOrderByAnalysisTimestampAsc(conversationId);
        
        if (emotionAnalyses.isEmpty()) {
            // 감정 분석 데이터가 없는 경우
            ConversationSummaryResponse.EmotionSummary summary = new ConversationSummaryResponse.EmotionSummary();
            // summary.setDominantEmotion("분석 없음"); // 기존 방식 - 주석처리
            summary.setEmotionCounts(new HashMap<>());
            // summary.setAverageConfidence(0.0); // 기존 방식 - 주석처리
            summary.setAnalyzedMessageCount(0);
            return summary;
        }
        
        // 감정별 개수 계산
        Map<String, Integer> emotionCounts = new HashMap<>();
        double totalConfidence = 0.0;
        int analyzedCount = 0;
        
        for (UserEmotionAnalysis analysis : emotionAnalyses) {
            if (analysis.getCombinedEmotion() != null && !analysis.getCombinedEmotion().isEmpty()) {
                String emotion = analysis.getCombinedEmotion();
                emotionCounts.put(emotion, emotionCounts.getOrDefault(emotion, 0) + 1);
                
                if (analysis.getCombinedConfidence() != null) {
                    totalConfidence += analysis.getCombinedConfidence();
                    analyzedCount++;
                }
            }
        }
        
        // TODO: 기존 감정 필드 사용 부분 - 새로운 감정 흐름 분석으로 대체 예정
        // 기존 dominantEmotion 계산 방식은 더 이상 사용하지 않음
        // 새로운 방식: EmotionFlowService.computeAndSaveFlow()에서 flowPattern과 emotionFlow 계산
        
        // 주요 감정 찾기 (기존 방식 - 주석처리)
        // String dominantEmotion = "중립";
        // int maxCount = 0;
        // for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
        //     if (entry.getValue() > maxCount) {
        //         maxCount = entry.getValue();
        //         dominantEmotion = entry.getKey();
        //     }
        // }
        
        // 평균 신뢰도 계산 (기존 방식 - 주석처리)
        // double averageConfidence = analyzedCount > 0 ? totalConfidence / analyzedCount : 0.0;
        
        // 응답 객체 생성
        ConversationSummaryResponse.EmotionSummary summary = new ConversationSummaryResponse.EmotionSummary();
        // summary.setDominantEmotion(dominantEmotion); // 기존 방식 - 주석처리
        summary.setEmotionCounts(emotionCounts);
        // summary.setAverageConfidence(averageConfidence); // 기존 방식 - 주석처리
        summary.setAnalyzedMessageCount(emotionAnalyses.size());
        
        return summary;
    }
    
    @Operation(summary = "대화 요약 저장", description = "생성된 대화 요약을 저장합니다")
    public void saveConversationSummary(Long conversationId, String summary) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setSummary(summary);
            conversationRepository.save(conversation);
        }
    }
    
    @Operation(summary = "대화 일기 저장", description = "생성된 일기를 저장합니다")
    public void saveConversationDiary(Long conversationId, String diary) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setDiary(diary);
            conversationRepository.save(conversation);
        }
    }
    
    @Operation(summary = "처리 상태 업데이트", description = "대화의 처리 상태를 업데이트합니다")
    public void updateProcessingStatus(Long conversationId, Conversation.ProcessingStatus status) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setProcessingStatus(status);
            conversationRepository.save(conversation);
        }
    }
    
    @Operation(summary = "처리 상태 조회", description = "대화의 처리 상태를 조회합니다")
    public Conversation.ProcessingStatus getProcessingStatus(Long conversationId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            return conversationOpt.get().getProcessingStatus();
        }
        return Conversation.ProcessingStatus.READY;
    }
    
    @Operation(summary = "대화 종료 및 백그라운드 처리 시작", description = "대화를 종료하고 백그라운드에서 요약 및 일기 생성을 시작합니다")
    public Conversation endConversationAndStartProcessing(Long conversationId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setStatus(Conversation.ConversationStatus.COMPLETED);
            conversation.setEndedAt(LocalDateTime.now());
            conversation.setProcessingStatus(Conversation.ProcessingStatus.READY);
            return conversationRepository.save(conversation);
        }
        return null;
    }
    
    @Operation(summary = "대화 감정 흐름 분석 결과 저장", description = "대화의 감정 흐름 분석 결과를 저장합니다")
    public void saveConversationFlowAnalysis(Long conversationId, String flowPattern, String emotionFlow) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setFlowPattern(flowPattern);
            conversation.setEmotionFlow(emotionFlow);
            conversationRepository.save(conversation);
        }
    }
} 