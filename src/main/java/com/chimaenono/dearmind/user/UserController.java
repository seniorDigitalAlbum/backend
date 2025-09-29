package com.chimaenono.dearmind.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 카카오 ID로 사용자 조회
     */
    @GetMapping("/kakao/{kakaoId}")
    @Operation(summary = "카카오 ID로 사용자 조회", description = "카카오 사용자 ID로 사용자 정보를 조회합니다.")
    public ResponseEntity<User> getUserByKakaoId(
            @Parameter(description = "카카오 사용자 ID", required = true)
            @PathVariable String kakaoId) {
        
        log.info("카카오 ID로 사용자 조회 요청: {}", kakaoId);
        
        Optional<User> user = userService.findByKakaoId(kakaoId);
        
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 ID로 조회
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 ID로 조회", description = "사용자 ID로 사용자 정보를 조회합니다.")
    public ResponseEntity<User> getUserById(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {
        
        log.info("사용자 ID로 조회 요청: {}", userId);
        
        Optional<User> user = userService.findById(userId);
        
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 닉네임 중복 확인
     */
    @GetMapping("/check-nickname/{nickname}")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 중복 여부를 확인합니다.")
    public ResponseEntity<Boolean> checkNicknameDuplicate(
            @Parameter(description = "확인할 닉네임", required = true)
            @PathVariable String nickname) {
        
        log.info("닉네임 중복 확인 요청: {}", nickname);
        
        boolean isDuplicate = userService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }
    
    
    /**
     * 카카오 ID 존재 확인
     */
    @GetMapping("/check-kakao/{kakaoId}")
    @Operation(summary = "카카오 ID 존재 확인", description = "카카오 사용자 ID의 존재 여부를 확인합니다.")
    public ResponseEntity<Boolean> checkKakaoIdExists(
            @Parameter(description = "확인할 카카오 사용자 ID", required = true)
            @PathVariable String kakaoId) {
        
        log.info("카카오 ID 존재 확인 요청: {}", kakaoId);
        
        boolean exists = userService.existsByKakaoId(kakaoId);
        return ResponseEntity.ok(exists);
    }
    
    /**
     * 사용자 정보 업데이트
     */
    @PutMapping("/{userId}")
    @Operation(summary = "사용자 정보 업데이트", description = "사용자의 정보를 업데이트합니다.")
    public ResponseEntity<User> updateUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        
        log.info("사용자 정보 업데이트 요청: userId={}", userId);
        
        try {
            User updatedUser = userService.updateUserInfo(
                userId,
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getGender(),
                request.getPhoneNumber()
            );
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("사용자 정보 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 계정 비활성화
     */
    @PutMapping("/{userId}/deactivate")
    @Operation(summary = "사용자 계정 비활성화", description = "사용자 계정을 비활성화합니다.")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {
        
        log.info("사용자 계정 비활성화 요청: userId={}", userId);
        
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("사용자 계정 비활성화 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 계정 활성화
     */
    @PutMapping("/{userId}/activate")
    @Operation(summary = "사용자 계정 활성화", description = "사용자 계정을 활성화합니다.")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {

        log.info("사용자 계정 활성화 요청: userId={}", userId);

        try {
            userService.activateUser(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("사용자 계정 활성화 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자 타입 업데이트
     */
    @PutMapping("/{userId}/user-type")
    @Operation(summary = "사용자 타입 업데이트", description = "사용자 타입을 설정합니다.")
    public ResponseEntity<User> updateUserType(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @RequestBody UserTypeRequest request) {

        log.info("사용자 타입 업데이트 요청: userId={}, userType={}", userId, request.getUserType());

        try {
            User updatedUser = userService.updateUserType(userId, request.getUserType());
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("사용자 타입 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 시니어 검색 (이름으로)
     */
    @GetMapping("/search/seniors/name")
    @Operation(summary = "이름으로 시니어 검색", description = "이름으로 시니어 사용자를 검색합니다.")
    public ResponseEntity<List<User>> searchSeniorsByName(
            @Parameter(description = "검색할 이름", required = true)
            @RequestParam String name) {

        log.info("이름으로 시니어 검색 요청: name={}", name);

        List<User> seniors = userService.searchSeniorsByName(name);
        return ResponseEntity.ok(seniors);
    }

    /**
     * 시니어 검색 (전화번호로)
     */
    @GetMapping("/search/seniors/phone")
    @Operation(summary = "전화번호로 시니어 검색", description = "전화번호로 시니어 사용자를 검색합니다.")
    public ResponseEntity<List<User>> searchSeniorsByPhoneNumber(
            @Parameter(description = "검색할 전화번호", required = true)
            @RequestParam String phoneNumber) {

        log.info("전화번호로 시니어 검색 요청: phoneNumber={}", phoneNumber);

        List<User> seniors = userService.searchSeniorsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(seniors);
    }

    /**
     * 시니어 통합 검색 (이름 또는 전화번호)
     */
    @GetMapping("/search/seniors")
    @Operation(summary = "시니어 통합 검색", description = "이름 또는 전화번호로 시니어 사용자를 검색합니다.")
    public ResponseEntity<List<User>> searchSeniors(
            @Parameter(description = "검색어 (이름 또는 전화번호)", required = true)
            @RequestParam String searchTerm) {

        log.info("시니어 통합 검색 요청: searchTerm={}", searchTerm);

        List<User> seniors = userService.searchSeniorsByNameOrPhoneNumber(searchTerm);
        return ResponseEntity.ok(seniors);
    }

    /**
     * 시니어 정확한 검색 (이름과 전화번호 모두 일치)
     */
    @GetMapping("/search/seniors/exact")
    @Operation(summary = "시니어 정확한 검색", description = "이름과 전화번호가 모두 일치하는 시니어를 검색합니다.")
    public ResponseEntity<List<User>> searchSeniorsExact(
            @Parameter(description = "검색할 이름", required = true)
            @RequestParam String name,
            @Parameter(description = "검색할 전화번호", required = true)
            @RequestParam String phoneNumber) {

        log.info("시니어 정확한 검색 요청: name={}, phoneNumber={}", name, phoneNumber);

        List<User> seniors = userService.searchSeniorsByNameAndPhoneNumber(name, phoneNumber);
        return ResponseEntity.ok(seniors);
    }
    
    /**
     * 사용자 정보 업데이트 요청 DTO
     */
    public static class UserUpdateRequest {
        private String nickname;
        private String profileImageUrl;
        private String gender;
        private String phoneNumber;
        
        // Getters and Setters
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    /**
     * 사용자 타입 업데이트 요청 DTO
     */
    public static class UserTypeRequest {
        private String userType;

        // Getters and Setters
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
    }
}
