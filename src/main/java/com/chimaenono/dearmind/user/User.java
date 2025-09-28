package com.chimaenono.dearmind.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 엔티티")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "사용자 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "kakao_id", unique = true, nullable = false)
    @Schema(description = "카카오 사용자 ID", example = "123456789")
    private String kakaoId;
    
    @Column(name = "nickname", nullable = false)
    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;
    
    @Column(name = "profile_image_url")
    @Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/dn/...")
    private String profileImageUrl;
    
    @Column(name = "gender")
    @Schema(description = "성별", example = "male", allowableValues = {"male", "female"})
    private String gender;
    
    @Column(name = "phone_number")
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;
    
    @Column(name = "user_type")
    @Schema(description = "사용자 타입", example = "SENIOR", allowableValues = {"SENIOR", "GUARDIAN"})
    private String userType; // SENIOR, GUARDIAN, null(미선택)
    
    @Column(name = "is_active", nullable = false)
    @Schema(description = "계정 활성화 상태", example = "true")
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "계정 생성일시")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "계정 수정일시")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login_at")
    @Schema(description = "마지막 로그인 일시")
    private LocalDateTime lastLoginAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 카카오 로그인 시 사용할 생성자
    public User(String kakaoId, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 마지막 로그인 시간 업데이트 메서드
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
