package com.chimaenono.dearmind.guardian;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.chimaenono.dearmind.user.User;

@Entity
@Table(name = "guardian_senior_relationships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "보호자-시니어 관계 엔티티")
public class GuardianSeniorRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "관계 고유 ID", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    @Schema(description = "보호자 사용자")
    private User guardian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senior_id", nullable = false)
    @Schema(description = "시니어 사용자")
    private User senior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "관계 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    private RelationshipStatus status = RelationshipStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "관계 생성일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "관계 최종 수정일시")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    @Schema(description = "관계 승인일시")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public GuardianSeniorRelationship(User guardian, User senior) {
        this.guardian = guardian;
        this.senior = senior;
        this.status = RelationshipStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = RelationshipStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = RelationshipStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    public enum RelationshipStatus {
        PENDING,    // 대기 중
        APPROVED,   // 승인됨
        REJECTED    // 거부됨
    }
}
