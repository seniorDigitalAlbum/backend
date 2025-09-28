package com.chimaenono.dearmind.microphone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.chimaenono.dearmind.camera.CameraSession;
import com.chimaenono.dearmind.camera.CameraSessionRepository;
import com.chimaenono.dearmind.conversationMessage.ConversationMessage;
import com.chimaenono.dearmind.conversationMessage.ConversationMessageRepository;
import com.chimaenono.dearmind.stt.STTService;
import com.chimaenono.dearmind.stt.STTResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Tag(name = "Microphone Service", description = "마이크 세션 관리 서비스")
public class MicrophoneService {

    @Autowired
    private MicrophoneSessionRepository microphoneSessionRepository;
    
    @Autowired
    private CameraSessionRepository cameraSessionRepository;
    
    @Autowired
    private ConversationMessageRepository conversationMessageRepository;
    
    @Autowired
    private STTService sttService;

    @Operation(summary = "마이크 세션 생성", description = "새로운 마이크 세션을 생성합니다")
    public MicrophoneSession createSession(Long userId, String audioFormat, Integer sampleRate) {
        MicrophoneSession session = new MicrophoneSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus("ACTIVE");
        session.setAudioFormat(audioFormat != null ? audioFormat : "WAV");
        session.setSampleRate(sampleRate != null ? sampleRate : 44100);
        return microphoneSessionRepository.save(session);
    }

    @Operation(summary = "세션 ID로 세션 조회", description = "세션 ID로 마이크 세션을 조회합니다")
    public Optional<MicrophoneSession> getSessionById(String sessionId) {
        return microphoneSessionRepository.findBySessionId(sessionId);
    }

    @Operation(summary = "사용자 세션 조회", description = "특정 사용자의 모든 마이크 세션을 조회합니다")
    public List<MicrophoneSession> getSessionsByUserId(Long userId) {
        return microphoneSessionRepository.findByUserId(userId);
    }

    @Operation(summary = "활성 세션 조회", description = "현재 활성 상태인 세션을 조회합니다")
    public List<MicrophoneSession> getActiveSessions() {
        return microphoneSessionRepository.findByStatus("ACTIVE");
    }

    @Operation(summary = "세션 상태 업데이트", description = "마이크 세션의 상태를 업데이트합니다")
    public MicrophoneSession updateSessionStatus(String sessionId, String status) {
        Optional<MicrophoneSession> sessionOpt = microphoneSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            MicrophoneSession session = sessionOpt.get();
            session.setStatus(status);
            if ("INACTIVE".equals(status)) {
                session.setEndedAt(LocalDateTime.now());
            }
            return microphoneSessionRepository.save(session);
        }
        return null;
    }

    @Operation(summary = "세션 종료", description = "마이크 세션을 종료합니다")
    public boolean endSession(String sessionId) {
        Optional<MicrophoneSession> sessionOpt = microphoneSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            MicrophoneSession session = sessionOpt.get();
            session.setStatus("INACTIVE");
            session.setEndedAt(LocalDateTime.now());
            microphoneSessionRepository.save(session);
            return true;
        }
        return false;
    }

    @Operation(summary = "사용자 활성 세션 조회", description = "특정 사용자의 활성 세션을 조회합니다")
    public Optional<MicrophoneSession> getActiveSessionByUserId(Long userId) {
        return microphoneSessionRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    @Operation(summary = "오디오 설정 업데이트", description = "마이크 세션의 오디오 설정을 업데이트합니다")
    public MicrophoneSession updateAudioSettings(String sessionId, String audioFormat, Integer sampleRate) {
        Optional<MicrophoneSession> sessionOpt = microphoneSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            MicrophoneSession session = sessionOpt.get();
            if (audioFormat != null) {
                session.setAudioFormat(audioFormat);
            }
            if (sampleRate != null) {
                session.setSampleRate(sampleRate);
            }
            return microphoneSessionRepository.save(session);
        }
        return null;
    }

    @Operation(summary = "발화 시작", description = "사용자의 발화를 시작합니다. 마이크와 카메라 세션 상태를 RECORDING으로 변경합니다.")
    public SpeechStartResponse startSpeech(SpeechStartRequest request) {
        // 마이크 세션 조회
        Optional<MicrophoneSession> microphoneSessionOpt = microphoneSessionRepository.findBySessionId(request.getMicrophoneSessionId());
        
        if (!microphoneSessionOpt.isPresent()) {
            throw new RuntimeException("마이크 세션을 찾을 수 없습니다: " + request.getMicrophoneSessionId());
        }
        
        MicrophoneSession microphoneSession = microphoneSessionOpt.get();
        
        // 카메라 세션 조회
        Optional<CameraSession> cameraSessionOpt = cameraSessionRepository.findBySessionId(request.getCameraSessionId());
        
        if (!cameraSessionOpt.isPresent()) {
            throw new RuntimeException("카메라 세션을 찾을 수 없습니다: " + request.getCameraSessionId());
        }
        
        CameraSession cameraSession = cameraSessionOpt.get();
        
        // 사용자 ID 검증 (마이크와 카메라 세션 모두)
        if (!microphoneSession.getUserId().equals(cameraSession.getUserId())) {
            throw new RuntimeException("마이크 세션과 카메라 세션의 사용자 ID가 일치하지 않습니다.");
        }
        
        // 현재 상태가 ACTIVE인지 확인 (마이크와 카메라 세션 모두)
        if (!"ACTIVE".equals(microphoneSession.getStatus())) {
            throw new RuntimeException("마이크 세션의 발화를 시작할 수 없습니다. 현재 상태: " + microphoneSession.getStatus());
        }
        
        if (!"ACTIVE".equals(cameraSession.getStatus())) {
            throw new RuntimeException("카메라 세션의 발화를 시작할 수 없습니다. 현재 상태: " + cameraSession.getStatus());
        }
        
        try {
            // 마이크 세션 상태를 RECORDING으로 변경
            microphoneSession.setStatus("RECORDING");
            MicrophoneSession updatedMicrophoneSession = microphoneSessionRepository.save(microphoneSession);
            
            // 카메라 세션 상태를 RECORDING으로 변경
            cameraSession.setStatus("RECORDING");
            cameraSessionRepository.save(cameraSession);
            
            // 응답 생성
            return SpeechStartResponse.from(updatedMicrophoneSession, request.getCameraSessionId(), request.getConversationId());
            
        } catch (Exception e) {
            // 롤백: 마이크 세션 상태 복원
            microphoneSession.setStatus("ACTIVE");
            microphoneSessionRepository.save(microphoneSession);
            
            // 롤백: 카메라 세션 상태 복원
            cameraSession.setStatus("ACTIVE");
            cameraSessionRepository.save(cameraSession);
            
            throw new RuntimeException("발화 시작 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "발화 종료", description = "사용자의 발화를 종료합니다. 마이크와 카메라 세션 상태를 ACTIVE로 변경하고 ConversationMessage를 생성합니다.")
    public SpeechEndResponse endSpeech(SpeechEndRequest request) {
        // 마이크 세션 조회
        Optional<MicrophoneSession> microphoneSessionOpt = microphoneSessionRepository.findBySessionId(request.getMicrophoneSessionId());
        
        if (!microphoneSessionOpt.isPresent()) {
            throw new RuntimeException("마이크 세션을 찾을 수 없습니다: " + request.getMicrophoneSessionId());
        }
        
        MicrophoneSession microphoneSession = microphoneSessionOpt.get();
        
        // 카메라 세션 조회
        Optional<CameraSession> cameraSessionOpt = cameraSessionRepository.findBySessionId(request.getCameraSessionId());
        
        if (!cameraSessionOpt.isPresent()) {
            throw new RuntimeException("카메라 세션을 찾을 수 없습니다: " + request.getCameraSessionId());
        }
        
        CameraSession cameraSession = cameraSessionOpt.get();
        
        // 사용자 ID 검증 (마이크와 카메라 세션 모두)
        if (!microphoneSession.getUserId().equals(cameraSession.getUserId())) {
            throw new RuntimeException("마이크 세션과 카메라 세션의 사용자 ID가 일치하지 않습니다.");
        }
        
        // 현재 상태가 RECORDING인지 확인 (마이크와 카메라 세션 모두)
        if (!"RECORDING".equals(microphoneSession.getStatus())) {
            throw new RuntimeException("마이크 세션의 발화를 종료할 수 없습니다. 현재 상태: " + microphoneSession.getStatus());
        }
        
        if (!"RECORDING".equals(cameraSession.getStatus())) {
            throw new RuntimeException("카메라 세션의 발화를 종료할 수 없습니다. 현재 상태: " + cameraSession.getStatus());
        }
        
        try {
            // 1. STT 처리 (오디오를 텍스트로 변환)
            STTResponse sttResponse = sttService.transcribeAudio(request.getAudioData(), "wav", "ko");
            
            if (!"success".equals(sttResponse.getStatus())) {
                throw new RuntimeException("STT 변환 실패: " + sttResponse.getError());
            }
            
            String userText = sttResponse.getText();
            if (userText == null || userText.trim().isEmpty()) {
                throw new RuntimeException("STT 변환 결과가 비어있습니다.");
            }
            
            // 2. ConversationMessage 생성 (사용자 발화 저장)
            ConversationMessage userMessage = new ConversationMessage();
            userMessage.setConversationId(request.getConversationId());
            userMessage.setContent(userText);
            userMessage.setSenderType(ConversationMessage.SenderType.USER);
            userMessage.setTimestamp(LocalDateTime.now());
            userMessage = conversationMessageRepository.save(userMessage);
            
            // 2. 마이크 세션 상태를 ACTIVE로 변경
            microphoneSession.setStatus("ACTIVE");
            microphoneSessionRepository.save(microphoneSession);
            
            // 3. 카메라 세션 상태를 ACTIVE로 변경
            cameraSession.setStatus("ACTIVE");
            cameraSessionRepository.save(cameraSession);
            
            // 4. 성공 응답 반환
            return SpeechEndResponse.success(
                userMessage.getId(),
                userText,
                request.getMicrophoneSessionId(),
                request.getCameraSessionId(),
                request.getConversationId()
            );
            
        } catch (Exception e) {
            // 롤백: 마이크 세션 상태 복원
            microphoneSession.setStatus("RECORDING");
            microphoneSessionRepository.save(microphoneSession);
            
            // 롤백: 카메라 세션 상태 복원
            cameraSession.setStatus("RECORDING");
            cameraSessionRepository.save(cameraSession);
            
            throw new RuntimeException("발화 종료 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 