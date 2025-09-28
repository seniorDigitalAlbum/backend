package com.chimaenono.dearmind.camera;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Tag(name = "Camera Service", description = "카메라 세션 관리 서비스")
public class CameraService {

    @Autowired
    private CameraSessionRepository cameraSessionRepository;

    @Operation(summary = "카메라 세션 생성", description = "새로운 카메라 세션을 생성합니다")
    public CameraSession createSession(Long userId) {
        CameraSession session = new CameraSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus("ACTIVE");
        return cameraSessionRepository.save(session);
    }

    @Operation(summary = "세션 ID로 세션 조회", description = "세션 ID로 카메라 세션을 조회합니다")
    public Optional<CameraSession> getSessionById(String sessionId) {
        return cameraSessionRepository.findBySessionId(sessionId);
    }

    @Operation(summary = "사용자 세션 조회", description = "특정 사용자의 모든 카메라 세션을 조회합니다")
    public List<CameraSession> getSessionsByUserId(Long userId) {
        return cameraSessionRepository.findByUserId(userId);
    }

    @Operation(summary = "활성 세션 조회", description = "현재 활성 상태인 세션을 조회합니다")
    public List<CameraSession> getActiveSessions() {
        return cameraSessionRepository.findByStatus("ACTIVE");
    }

    @Operation(summary = "세션 상태 업데이트", description = "카메라 세션의 상태를 업데이트합니다")
    public CameraSession updateSessionStatus(String sessionId, String status) {
        Optional<CameraSession> sessionOpt = cameraSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            CameraSession session = sessionOpt.get();
            session.setStatus(status);
            if ("INACTIVE".equals(status)) {
                session.setEndedAt(LocalDateTime.now());
            }
            return cameraSessionRepository.save(session);
        }
        return null;
    }

    @Operation(summary = "세션 종료", description = "카메라 세션을 종료합니다")
    public boolean endSession(String sessionId) {
        Optional<CameraSession> sessionOpt = cameraSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            CameraSession session = sessionOpt.get();
            session.setStatus("INACTIVE");
            session.setEndedAt(LocalDateTime.now());
            cameraSessionRepository.save(session);
            return true;
        }
        return false;
    }

    @Operation(summary = "사용자 활성 세션 조회", description = "특정 사용자의 활성 세션을 조회합니다")
    public Optional<CameraSession> getActiveSessionByUserId(Long userId) {
        return cameraSessionRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }
} 