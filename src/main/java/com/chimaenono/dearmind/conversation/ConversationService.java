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
    public ConversationMessage saveUserMessage(Long conversationId, String content, String audioFilePath, String videoFilePath) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setSenderType(ConversationMessage.SenderType.USER);
        message.setContent(content);
        message.setAudioFilePath(audioFilePath);
        message.setVideoFilePath(videoFilePath);
        return saveMessage(message);
    }
    
    @Operation(summary = "AI 메시지 저장", description = "AI의 메시지를 저장합니다")
    public ConversationMessage saveAIMessage(Long conversationId, String content, String audioFilePath, String videoFilePath) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setSenderType(ConversationMessage.SenderType.AI);
        message.setContent(content);
        message.setAudioFilePath(audioFilePath);
        message.setVideoFilePath(videoFilePath);
        return saveMessage(message);
    }
} 