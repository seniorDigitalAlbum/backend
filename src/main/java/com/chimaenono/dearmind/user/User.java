// src/main/java/com/chimaenono/dearmind/user/User.java
package com.chimaenono.dearmind.user;

import com.chimaenono.dearmind.auth.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 엔티티")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 서비스 내 식별자(외부 노출용). 카카오/전화가입 공통으로 사용 */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

    /** ⚠️ 민감정보: 응답에서 숨김 */
    @JsonIgnore
    @Column(name = "kakao_access_token", columnDefinition = "TEXT")
    private String kakaoAccessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    /** 스프링 시큐리티 권한 */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** OAuth로 받아온 정보 반영 */
    public User update(String name, String picture) {
        this.name = name;
        this.profileImage = picture;
        return this;
    }

    /** GrantedAuthority 생성에 사용 */
    public String getRoleKey() {
        return this.role.getKey();
    }

    @PrePersist
    protected void onCreate() {
        // ✅ 기본값 보장
        if (this.role == null) this.role = Role.USER;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (this.role == null) this.role = Role.USER; // 안전망
    }

    public enum UserType {
        SENIOR,
        GUARDIAN
    }

    public enum Gender {
        MALE,
        FEMALE
    }
}
