package com.chimaenono.dearmind.microphone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Tag(name = "Microphone Service", description = "마이크 세션 관리 서비스")
public class MicrophoneService {

    @Autowired
    private MicrophoneSessionRepository microphoneSessionRepository;

    @Operation(summary = "마이크 세션 생성", description = "새로운 마이크 세션을 생성합니다")
    public MicrophoneSession createSession(String userId, String audioFormat, Integer sampleRate) {
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
    public List<MicrophoneSession> getSessionsByUserId(String userId) {
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
    public Optional<MicrophoneSession> getActiveSessionByUserId(String userId) {
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
} 