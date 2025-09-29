package com.chimaenono.dearmind.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * 카카오 ID로 사용자 조회
     * @param kakaoId 카카오 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    public Optional<User> findByKakaoId(String kakaoId) {
        log.info("카카오 ID로 사용자 조회: {}", kakaoId);
        return userRepository.findByKakaoId(kakaoId);
    }
    
    /**
     * 활성화된 사용자 조회
     * @param kakaoId 카카오 사용자 ID
     * @return 활성화된 사용자 정보 (Optional)
     */
    public Optional<User> findActiveUserByKakaoId(String kakaoId) {
        log.info("활성화된 사용자 조회: {}", kakaoId);
        return userRepository.findActiveUserByKakaoId(kakaoId);
    }
    
    /**
     * 사용자 ID로 조회
     * @param userId 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    public Optional<User> findById(Long userId) {
        log.info("사용자 ID로 조회: {}", userId);
        return userRepository.findById(userId);
    }
    
    /**
     * 카카오 로그인 시 사용자 생성 또는 업데이트
     * @param kakaoId 카카오 사용자 ID
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 사용자 정보
     */
    @Transactional
    public User createOrUpdateUser(String kakaoId, String nickname, String profileImageUrl) {
        log.info("사용자 생성 또는 업데이트: kakaoId={}, nickname={}", kakaoId, nickname);
        
        Optional<User> existingUser = userRepository.findByKakaoId(kakaoId);
        
        if (existingUser.isPresent()) {
            // 기존 사용자 정보 업데이트
            User user = existingUser.get();
            user.setNickname(nickname);
            user.setProfileImageUrl(profileImageUrl);
            user.updateLastLogin();
            user.setIsActive(true);
            
            log.info("기존 사용자 정보 업데이트: userId={}", user.getId());
            return userRepository.save(user);
        } else {
            // 새 사용자 생성
            User newUser = new User(kakaoId, nickname, profileImageUrl);
            log.info("새 사용자 생성: kakaoId={}", kakaoId);
            return userRepository.save(newUser);
        }
    }
    
    /**
     * 사용자 정보 업데이트
     * @param userId 사용자 ID
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @param gender 성별
     * @param phoneNumber 전화번호
     * @return 업데이트된 사용자 정보
     */
    @Transactional
    public User updateUserInfo(Long userId, String nickname, String profileImageUrl, 
                              String gender, String phoneNumber) {
        log.info("사용자 정보 업데이트: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        if (nickname != null) user.setNickname(nickname);
        if (profileImageUrl != null) user.setProfileImageUrl(profileImageUrl);
        if (gender != null) user.setGender(gender);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        
        return userRepository.save(user);
    }
    
    /**
     * 사용자 계정 비활성화
     * @param userId 사용자 ID
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("사용자 계정 비활성화: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setIsActive(false);
        userRepository.save(user);
    }
    
    /**
     * 사용자 계정 활성화
     * @param userId 사용자 ID
     */
    @Transactional
    public void activateUser(Long userId) {
        log.info("사용자 계정 활성화: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setIsActive(true);
        userRepository.save(user);
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     * @param userId 사용자 ID
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        log.info("마지막 로그인 시간 업데이트: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        user.updateLastLogin();
        userRepository.save(user);
    }
    
    /**
     * 닉네임 중복 확인
     * @param nickname 닉네임
     * @return 중복 여부
     */
    public boolean isNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
    
    
    /**
     * 카카오 ID 존재 확인
     * @param kakaoId 카카오 사용자 ID
     * @return 존재 여부
     */
    public boolean existsByKakaoId(String kakaoId) {
        return userRepository.existsByKakaoId(kakaoId);
    }
    
    /**
     * 사용자 타입 업데이트
     * @param userId 사용자 ID
     * @param userType 사용자 타입 (SENIOR, GUARDIAN)
     * @return 업데이트된 사용자 정보
     */
    @Transactional
    public User updateUserType(Long userId, String userType) {
        log.info("사용자 타입 업데이트: userId={}, userType={}", userId, userType);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setUserType(userType);
        return userRepository.save(user);
    }
    
    /**
     * 이름으로 시니어 검색
     * @param name 검색할 이름
     * @return 시니어 사용자 목록
     */
    public List<User> searchSeniorsByName(String name) {
        log.info("이름으로 시니어 검색: name={}", name);
        return userRepository.findSeniorsByName(name);
    }
    
    /**
     * 전화번호로 시니어 검색
     * @param phoneNumber 검색할 전화번호
     * @return 시니어 사용자 목록
     */
    public List<User> searchSeniorsByPhoneNumber(String phoneNumber) {
        log.info("전화번호로 시니어 검색: phoneNumber={}", phoneNumber);
        return userRepository.findSeniorsByPhoneNumber(phoneNumber);
    }
    
    /**
     * 이름 또는 전화번호로 시니어 통합 검색
     * @param searchTerm 검색어 (이름 또는 전화번호)
     * @return 시니어 사용자 목록
     */
    public List<User> searchSeniorsByNameOrPhoneNumber(String searchTerm) {
        log.info("이름 또는 전화번호로 시니어 통합 검색: searchTerm={}", searchTerm);
        return userRepository.findSeniorsByNameOrPhoneNumber(searchTerm);
    }
    
    /**
     * 이름과 전화번호 모두 일치하는 시니어 검색
     * @param name 검색할 이름
     * @param phoneNumber 검색할 전화번호
     * @return 시니어 사용자 목록
     */
    public List<User> searchSeniorsByNameAndPhoneNumber(String name, String phoneNumber) {
        log.info("이름과 전화번호 모두 일치하는 시니어 검색: name={}, phoneNumber={}", name, phoneNumber);
        return userRepository.findSeniorsByNameAndPhoneNumber(name, phoneNumber);
    }
}
