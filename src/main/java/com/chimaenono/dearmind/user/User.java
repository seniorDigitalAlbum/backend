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
    
    @Column(name = "user_id", nullable = false, unique = true)
    @Schema(description = "사용자 ID", example = "user_123")
    private String userId;
    
    @Column(name = "name", nullable = false)
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    
    
    @Column(name = "phone")
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;
    
    @Column(name = "kakao_id")
    @Schema(description = "카카오 사용자 ID", example = "123456789")
    private String kakaoId;
    
    @Column(name = "kakao_access_token", columnDefinition = "TEXT")
    @Schema(description = "카카오 액세스 토큰")
    private String kakaoAccessToken;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    @Schema(description = "성별", example = "MALE")
    private Gender gender;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = true)
    @Schema(description = "사용자 타입", example = "SENIOR")
    private UserType userType;
    
    @Column(name = "profile_image")
    @Schema(description = "프로필 이미지 URL")
    private String profileImage;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "계정 생성 시간")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "계정 수정 시간")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum UserType {
        SENIOR,    // 시니어 사용자
        GUARDIAN   // 보호자
    }
    
    public enum Gender {
        MALE,      // 남성
        FEMALE     // 여성
    }
}
