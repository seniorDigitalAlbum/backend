package com.chimaenono.dearmind.microphone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MicrophoneSessionRepository extends JpaRepository<MicrophoneSession, Long> {
    Optional<MicrophoneSession> findBySessionId(String sessionId);
    List<MicrophoneSession> findByUserId(String userId);
    List<MicrophoneSession> findByStatus(String status);
    Optional<MicrophoneSession> findByUserIdAndStatus(String userId, String status);
} 