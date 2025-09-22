package com.chimaenono.dearmind.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "사용자 관리 API")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getUserType()
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 조회", description = "사용자 ID로 사용자를 조회합니다")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        Optional<User> user = userService.getUserByUserId(userId);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "이메일로 사용자 조회", description = "이메일로 사용자를 조회합니다")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{userType}")
    @Operation(summary = "사용자 타입별 조회", description = "사용자 타입별로 사용자 목록을 조회합니다")
    public ResponseEntity<List<User>> getUsersByType(@PathVariable User.UserType userType) {
        List<User> users = userService.getUsersByType(userType);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 정보 업데이트", description = "사용자 정보를 업데이트합니다")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
        try {
            User user = userService.updateUser(
                userId,
                request.getName(),
                request.getPhone(),
                request.getProfileImage()
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check-email/{email}")
    @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.isEmailExists(email);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "전화번호로 사용자 조회", description = "전화번호로 사용자를 조회합니다")
    public ResponseEntity<User> getUserByPhone(@PathVariable String phone) {
        Optional<User> user = userService.getUserByPhone(phone);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check-phone/{phone}")
    @Operation(summary = "전화번호 중복 확인", description = "전화번호 중복 여부를 확인합니다")
    public ResponseEntity<Boolean> checkPhoneExists(@PathVariable String phone) {
        boolean exists = userService.isPhoneExists(phone);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/link")
    @Operation(summary = "보호자-시니어 연결", description = "보호자와 시니어를 연결합니다")
    public ResponseEntity<UserLink> linkUsers(@RequestBody LinkUserRequest request) {
        try {
            UserLink userLink = userService.linkGuardianToSenior(
                request.getGuardianUserId(), 
                request.getSeniorPhoneNumber()
            );
            return ResponseEntity.ok(userLink);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{userId}/linked-seniors")
    @Operation(summary = "보호자의 연결된 시니어 조회", description = "보호자가 연결한 시니어 목록을 조회합니다")
    public ResponseEntity<List<UserLink>> getLinkedSeniors(@PathVariable String userId) {
        List<UserLink> links = userService.getLinkedSeniors(userId);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/{userId}/linked-guardians")
    @Operation(summary = "시니어의 연결된 보호자 조회", description = "시니어를 연결한 보호자 목록을 조회합니다")
    public ResponseEntity<List<UserLink>> getLinkedGuardians(@PathVariable String userId) {
        List<UserLink> links = userService.getLinkedGuardians(userId);
        return ResponseEntity.ok(links);
    }

    @DeleteMapping("/link")
    @Operation(summary = "사용자 연결 해제", description = "보호자와 시니어의 연결을 해제합니다")
    public ResponseEntity<Void> unlinkUsers(@RequestBody UnlinkUserRequest request) {
        try {
            userService.unlinkUsers(request.getGuardianUserId(), request.getSeniorUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // DTO 클래스들
    public static class CreateUserRequest {
        private String name;
        private String email;
        private String phone;
        private User.UserType userType;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public User.UserType getUserType() { return userType; }
        public void setUserType(User.UserType userType) { this.userType = userType; }
    }

    public static class UpdateUserRequest {
        private String name;
        private String phone;
        private String profileImage;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getProfileImage() { return profileImage; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    }

    public static class LinkUserRequest {
        private String guardianUserId;
        private String seniorPhoneNumber;

        // Getters and Setters
        public String getGuardianUserId() { return guardianUserId; }
        public void setGuardianUserId(String guardianUserId) { this.guardianUserId = guardianUserId; }
        public String getSeniorPhoneNumber() { return seniorPhoneNumber; }
        public void setSeniorPhoneNumber(String seniorPhoneNumber) { this.seniorPhoneNumber = seniorPhoneNumber; }
    }

    public static class UnlinkUserRequest {
        private String guardianUserId;
        private String seniorUserId;

        // Getters and Setters
        public String getGuardianUserId() { return guardianUserId; }
        public void setGuardianUserId(String guardianUserId) { this.guardianUserId = guardianUserId; }
        public String getSeniorUserId() { return seniorUserId; }
        public void setSeniorUserId(String seniorUserId) { this.seniorUserId = seniorUserId; }
    }
}
