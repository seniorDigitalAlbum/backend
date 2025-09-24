package com.chimaenono.dearmind.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Slf4j
@Service
@Tag(name = "User Service", description = "사용자 관리 서비스")
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserLinkRepository userLinkRepository;

    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다")
    public User createUser(String name, String phone, User.UserType userType) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setName(name);
        user.setPhone(phone);
        user.setUserType(userType);
        return userRepository.save(user);
    }

    @Operation(summary = "카카오 사용자 생성", description = "카카오 로그인으로 새로운 사용자를 생성합니다")
    public User createKakaoUser(String kakaoId, String name, User.UserType userType, User.Gender gender, String profileImage, String kakaoAccessToken) {
        User user = new User();
        user.setUserId("kakao_" + kakaoId); // 카카오 ID를 userId로 사용
        user.setKakaoId(kakaoId);
        user.setName(name);
        user.setUserType(userType);
        user.setGender(gender);
        user.setProfileImage(profileImage);
        user.setKakaoAccessToken(kakaoAccessToken);
        return userRepository.save(user);
    }

    @Operation(summary = "카카오 사용자 조회", description = "카카오 ID로 사용자를 조회합니다")
    public Optional<User> getUserByKakaoId(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId);
    }

    @Operation(summary = "사용자 ID로 조회", description = "사용자 ID로 사용자를 조회합니다")
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }


    @Operation(summary = "사용자 타입별 조회", description = "사용자 타입별로 사용자 목록을 조회합니다")
    public List<User> getUsersByType(User.UserType userType) {
        return userRepository.findByUserType(userType);
    }

    @Operation(summary = "사용자 정보 업데이트", description = "사용자 정보를 업데이트합니다")
    public User updateUser(String userId, String name, String phone, String profileImage) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setName(name);
            user.setPhone(phone);
            if (profileImage != null) {
                user.setProfileImage(profileImage);
            }
            return userRepository.save(user);
        }
        throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
    }
    
    @Operation(summary = "카카오 액세스 토큰 업데이트", description = "사용자의 카카오 액세스 토큰을 업데이트합니다")
    public User updateKakaoAccessToken(String userId, String kakaoAccessToken) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setKakaoAccessToken(kakaoAccessToken);
            return userRepository.save(user);
        }
        throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
    }


    @Operation(summary = "사용자 ID 중복 확인", description = "사용자 ID 중복 여부를 확인합니다")
    public boolean isUserIdExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @Operation(summary = "전화번호로 사용자 조회", description = "전화번호로 사용자를 조회합니다")
    public Optional<User> getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Operation(summary = "전화번호 중복 확인", description = "전화번호 중복 여부를 확인합니다")
    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Operation(summary = "전화번호로 사용자 생성", description = "전화번호 기반으로 사용자를 생성합니다")
    public User createUserByPhone(String phone, User.UserType userType, String gender, String seniorPhoneNumber) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setPhone(phone);
        user.setUserType(userType);
        
        // 시니어인 경우 성별 설정
        if (userType == User.UserType.SENIOR && gender != null) {
            // User 엔티티에 gender 필드가 있다면 설정
            // user.setGender(gender);
        }
        
        return userRepository.save(user);
    }

    @Operation(summary = "보호자-시니어 연결", description = "보호자와 시니어를 연결합니다")
    public UserLink linkGuardianToSenior(String guardianUserId, String seniorPhoneNumber) {
        // 보호자 조회
        Optional<User> guardianOpt = userRepository.findByUserId(guardianUserId);
        if (guardianOpt.isEmpty()) {
            throw new RuntimeException("보호자를 찾을 수 없습니다.");
        }
        
        // 시니어 전화번호로 시니어 조회
        Optional<User> seniorOpt = userRepository.findByPhone(seniorPhoneNumber);
        if (seniorOpt.isPresent()) {
            User guardian = guardianOpt.get();
            User senior = seniorOpt.get();
            
            // 이미 연결되어 있는지 확인
            if (userLinkRepository.existsByGuardianIdAndSeniorId(guardian.getId(), senior.getId())) {
                throw new RuntimeException("이미 연결된 사용자입니다.");
            }
            
            // UserLink 생성 및 저장
            UserLink userLink = new UserLink();
            userLink.setGuardianId(guardian.getId());
            userLink.setSeniorId(senior.getId());
            userLink.setStatus("ACTIVE");
            
            return userLinkRepository.save(userLink);
        } else {
            throw new RuntimeException("해당 전화번호로 가입된 시니어를 찾을 수 없습니다.");
        }
    }

    @Operation(summary = "보호자의 연결된 시니어 조회", description = "보호자가 연결한 시니어 목록을 조회합니다")
    public List<UserLink> getLinkedSeniors(String guardianUserId) {
        Optional<User> guardianOpt = userRepository.findByUserId(guardianUserId);
        if (guardianOpt.isEmpty()) {
            return List.of();
        }
        return userLinkRepository.findByGuardianId(guardianOpt.get().getId());
    }

    @Operation(summary = "시니어의 연결된 보호자 조회", description = "시니어를 연결한 보호자 목록을 조회합니다")
    public List<UserLink> getLinkedGuardians(String seniorUserId) {
        Optional<User> seniorOpt = userRepository.findByUserId(seniorUserId);
        if (seniorOpt.isEmpty()) {
            return List.of();
        }
        return userLinkRepository.findBySeniorId(seniorOpt.get().getId());
    }

    @Operation(summary = "사용자 연결 해제", description = "보호자와 시니어의 연결을 해제합니다")
    public void unlinkUsers(String guardianUserId, String seniorUserId) {
        Optional<User> guardianOpt = userRepository.findByUserId(guardianUserId);
        Optional<User> seniorOpt = userRepository.findByUserId(seniorUserId);
        
        if (guardianOpt.isEmpty() || seniorOpt.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        Optional<UserLink> linkOpt = userLinkRepository.findByGuardianIdAndSeniorId(
            guardianOpt.get().getId(), seniorOpt.get().getId());
        if (linkOpt.isPresent()) {
            UserLink userLink = linkOpt.get();
            userLink.setStatus("INACTIVE");
            userLinkRepository.save(userLink);
        } else {
            throw new RuntimeException("연결된 사용자를 찾을 수 없습니다.");
        }
    }

    /**
     * 전화번호 목록으로 특정 사용자 타입의 사용자들을 검색
     */
    public List<User> findByPhoneNumbersAndUserType(List<String> phoneNumbers, String userType) {
        return userRepository.findByPhoneInAndUserType(phoneNumbers, User.UserType.valueOf(userType));
    }

    /**
     * 카카오 ID 목록으로 특정 사용자 타입의 사용자들을 검색
     */
    public List<User> findByKakaoIdsAndUserType(List<Long> kakaoIds, String userType) {
        List<String> kakaoIdStrings = kakaoIds.stream()
            .map(String::valueOf)
            .collect(java.util.stream.Collectors.toList());
        return userRepository.findByKakaoIdInAndUserType(kakaoIdStrings, User.UserType.valueOf(userType));
    }

    /**
     * 보호자와 시니어 연결
     */
    public boolean connectGuardianAndSenior(Long guardianId, Long seniorId) {
        try {
            // ID로 사용자 조회
            Optional<User> guardianOpt = userRepository.findById(guardianId);
            Optional<User> seniorOpt = userRepository.findById(seniorId);
            
            if (guardianOpt.isEmpty() || seniorOpt.isEmpty()) {
                log.error("사용자를 찾을 수 없습니다: guardianId={}, seniorId={}", guardianId, seniorId);
                return false;
            }
            
            User guardian = guardianOpt.get();
            User senior = seniorOpt.get();
            
            // 기존 연결 확인 (DB ID로)
            Optional<UserLink> existingLink = userLinkRepository.findByGuardianIdAndSeniorId(guardianId, seniorId);
            if (existingLink.isPresent()) {
                return true; // 이미 연결됨
            }

            // 새로운 연결 생성
            UserLink userLink = new UserLink();
            userLink.setGuardianId(guardianId);
            userLink.setSeniorId(seniorId);
            userLinkRepository.save(userLink);
            
            return true;
        } catch (Exception e) {
            log.error("사용자 연결 실패: guardianId={}, seniorId={}", guardianId, seniorId, e);
            return false;
        }
    }

    /**
     * 보호자와 연결된 시니어 목록 조회
     */
    public List<User> getConnectedSeniorsByGuardianId(Long guardianId) {
        try {
            // 보호자 정보 조회
            Optional<User> guardianOpt = userRepository.findById(guardianId);
            if (guardianOpt.isEmpty()) {
                log.error("보호자를 찾을 수 없습니다: guardianId={}", guardianId);
                return List.of();
            }
            
            // 연결된 시니어 UserLink 조회
            List<UserLink> userLinks = userLinkRepository.findByGuardianId(guardianId);
            
            // 연결된 시니어들의 ID 목록 추출
            List<Long> seniorIds = userLinks.stream()
                .map(UserLink::getSeniorId)
                .collect(java.util.stream.Collectors.toList());
            
            // 시니어 정보 조회
            List<User> seniors = new ArrayList<>();
            for (Long seniorId : seniorIds) {
                Optional<User> seniorOpt = userRepository.findById(seniorId);
                if (seniorOpt.isPresent()) {
                    seniors.add(seniorOpt.get());
                }
            }
            
            return seniors;
        } catch (Exception e) {
            log.error("연결된 시니어 목록 조회 실패: guardianId={}", guardianId, e);
            return List.of();
        }
    }
}
