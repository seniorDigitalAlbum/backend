package com.chimaenono.dearmind.camera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraSessionRepository extends JpaRepository<CameraSession, Long> {
    Optional<CameraSession> findBySessionId(String sessionId);
    List<CameraSession> findByUserId(Long userId);
    List<CameraSession> findByStatus(String status);
    Optional<CameraSession> findByUserIdAndStatus(Long userId, String status);
} 