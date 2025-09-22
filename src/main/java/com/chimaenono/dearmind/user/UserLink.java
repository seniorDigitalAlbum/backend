package com.chimaenono.dearmind.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 연결 엔티티 (보호자-시니어)")
public class UserLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "연결 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "guardian_user_id", nullable = false)
    @Schema(description = "보호자 사용자 ID", example = "guardian_123")
    private String guardianUserId;
    
    @Column(name = "senior_user_id", nullable = false)
    @Schema(description = "시니어 사용자 ID", example = "senior_456")
    private String seniorUserId;
    
    @Column(name = "status", nullable = false)
    @Schema(description = "연결 상태", example = "ACTIVE")
    private String status; // ACTIVE, INACTIVE, PENDING
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "연결 생성 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "연결 수정 시간")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "ACTIVE";
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
