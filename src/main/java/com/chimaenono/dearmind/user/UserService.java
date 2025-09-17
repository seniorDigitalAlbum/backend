package com.chimaenono.dearmind.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Tag(name = "User Service", description = "사용자 관리 서비스")
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserLinkRepository userLinkRepository;

    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다")
    public User createUser(String name, String email, String phone, User.UserType userType) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUserType(userType);
        return userRepository.save(user);
    }

    @Operation(summary = "사용자 ID로 조회", description = "사용자 ID로 사용자를 조회합니다")
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    @Operation(summary = "이메일로 조회", description = "이메일로 사용자를 조회합니다")
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
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

    @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다")
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
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
        // 시니어 전화번호로 시니어 조회
        Optional<User> seniorOpt = userRepository.findByPhone(seniorPhoneNumber);
        if (seniorOpt.isPresent()) {
            User senior = seniorOpt.get();
            
            // 이미 연결되어 있는지 확인
            if (userLinkRepository.existsByGuardianUserIdAndSeniorUserId(guardianUserId, senior.getUserId())) {
                throw new RuntimeException("이미 연결된 사용자입니다.");
            }
            
            // UserLink 생성 및 저장
            UserLink userLink = new UserLink();
            userLink.setGuardianUserId(guardianUserId);
            userLink.setSeniorUserId(senior.getUserId());
            userLink.setStatus("ACTIVE");
            
            return userLinkRepository.save(userLink);
        } else {
            throw new RuntimeException("해당 전화번호로 가입된 시니어를 찾을 수 없습니다.");
        }
    }

    @Operation(summary = "보호자의 연결된 시니어 조회", description = "보호자가 연결한 시니어 목록을 조회합니다")
    public List<UserLink> getLinkedSeniors(String guardianUserId) {
        return userLinkRepository.findByGuardianUserId(guardianUserId);
    }

    @Operation(summary = "시니어의 연결된 보호자 조회", description = "시니어를 연결한 보호자 목록을 조회합니다")
    public List<UserLink> getLinkedGuardians(String seniorUserId) {
        return userLinkRepository.findBySeniorUserId(seniorUserId);
    }

    @Operation(summary = "사용자 연결 해제", description = "보호자와 시니어의 연결을 해제합니다")
    public void unlinkUsers(String guardianUserId, String seniorUserId) {
        Optional<UserLink> linkOpt = userLinkRepository.findByGuardianUserIdAndSeniorUserId(guardianUserId, seniorUserId);
        if (linkOpt.isPresent()) {
            UserLink userLink = linkOpt.get();
            userLink.setStatus("INACTIVE");
            userLinkRepository.save(userLink);
        } else {
            throw new RuntimeException("연결된 사용자를 찾을 수 없습니다.");
        }
    }
}
