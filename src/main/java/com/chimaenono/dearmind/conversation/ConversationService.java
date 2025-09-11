package com.chimaenono.dearmind.conversation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Tag(name = "Conversation Service", description = "대화 세션 관리 서비스")
public class ConversationService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Operation(summary = "대화 세션 생성", description = "새로운 대화 세션을 생성합니다")
    public Conversation createConversation(String userId, Long questionId, String cameraSessionId, String microphoneSessionId) {
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
    
    @Operation(summary = "사용자별 대화 세션 조회", description = "사용자의 모든 대화 세션을 최신순으로 조회합니다")
    public List<Conversation> getConversationsByUserId(String userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Operation(summary = "사용자의 활성 대화 세션 조회", description = "사용자의 활성 상태 대화 세션을 조회합니다")
    public Optional<Conversation> getActiveConversationByUserId(String userId) {
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
    public void createDummyConversations(String userId) {
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
} 