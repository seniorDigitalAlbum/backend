package com.chimaenono.dearmind.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import com.chimaenono.dearmind.user.User.UserType;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Controller", description = "인증 관련 API")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "전화번호 기반 회원가입 (테스트용: 인증 없이 진행)")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        try {
            // 테스트용: 전화번호 중복 확인을 건너뛰고 바로 진행
            System.out.println("🧪 테스트 모드 - 회원가입 요청: " + request.getPhoneNumber());
            
            // 실제 운영 시에는 아래 주석을 해제하고 위의 테스트 로그를 제거
            /*
            if (userService.isPhoneExists(request.getPhoneNumber())) {
                return ResponseEntity.badRequest()
                    .body(new SignUpResponse(false, "이미 가입된 전화번호입니다.", null));
            }
            */

            // 테스트용: 보호자-시니어 연결 확인도 건너뛰기
            // 실제 운영 시에는 아래 주석을 해제
            /*
            if (request.getUserType() == UserType.GUARDIAN && request.getSeniorPhoneNumber() != null) {
                if (!userService.isPhoneExists(request.getSeniorPhoneNumber())) {
                    return ResponseEntity.badRequest()
                        .body(new SignUpResponse(false, "해당 전화번호로 가입된 시니어를 찾을 수 없습니다.", null));
                }
            }
            */

            // 사용자 생성
            User user = userService.createUserByPhone(
                request.getPhoneNumber(),
                request.getUserType(),
                request.getGender(),
                request.getSeniorPhoneNumber()
            );

            // 테스트용: 보호자-시니어 연결도 건너뛰기
            // 실제 운영 시에는 아래 주석을 해제
            /*
            if (request.getUserType() == UserType.GUARDIAN && request.getSeniorPhoneNumber() != null) {
                try {
                    userService.linkGuardianToSenior(user.getUserId(), request.getSeniorPhoneNumber());
                } catch (Exception e) {
                    // 연결 실패 시에도 회원가입은 성공으로 처리
                    System.err.println("사용자 연결 실패: " + e.getMessage());
                }
            }
            */

            SignUpResponse response = new SignUpResponse(
                true,
                "🧪 테스트 모드: 회원가입이 완료되었습니다.",
                new SignUpData(user.getUserId(), user.getUserType(), user.getPhone())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new SignUpResponse(false, "회원가입에 실패했습니다: " + e.getMessage(), null));
        }
    }

    @PostMapping("/link-senior")
    @Operation(summary = "시니어 추가 연결", description = "보호자가 새로운 시니어를 추가로 연결합니다")
    public ResponseEntity<LinkSeniorResponse> linkSenior(@RequestBody LinkSeniorRequest request) {
        try {
            // 보호자-시니어 연결
            userService.linkGuardianToSenior(request.getGuardianUserId(), request.getSeniorPhoneNumber());
            
            LinkSeniorResponse response = new LinkSeniorResponse(
                true,
                "시니어가 성공적으로 연결되었습니다.",
                null
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LinkSeniorResponse(false, "시니어 연결에 실패했습니다: " + e.getMessage(), null));
        }
    }

    @GetMapping("/linked-seniors/{guardianUserId}")
    @Operation(summary = "연결된 시니어 목록 조회", description = "보호자가 연결한 시니어 목록을 조회합니다")
    public ResponseEntity<LinkedSeniorsResponse> getLinkedSeniors(@PathVariable String guardianUserId) {
        try {
            var linkedSeniors = userService.getLinkedSeniors(guardianUserId);
            
            LinkedSeniorsResponse response = new LinkedSeniorsResponse(
                true,
                "연결된 시니어 목록을 조회했습니다.",
                linkedSeniors
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LinkedSeniorsResponse(false, "시니어 목록 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // DTO 클래스들
    public static class SignUpRequest {
        private String phoneNumber;
        private UserType userType;
        private String gender; // MALE, FEMALE
        private String seniorPhoneNumber; // 보호자만

        // Getters and Setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public UserType getUserType() { return userType; }
        public void setUserType(UserType userType) { this.userType = userType; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getSeniorPhoneNumber() { return seniorPhoneNumber; }
        public void setSeniorPhoneNumber(String seniorPhoneNumber) { this.seniorPhoneNumber = seniorPhoneNumber; }
    }

    public static class SignUpResponse {
        private boolean success;
        private String message;
        private SignUpData data;

        public SignUpResponse(boolean success, String message, SignUpData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public SignUpData getData() { return data; }
        public void setData(SignUpData data) { this.data = data; }
    }

    public static class SignUpData {
        private String userId;
        private UserType userType;
        private String phoneNumber;

        public SignUpData(String userId, UserType userType, String phoneNumber) {
            this.userId = userId;
            this.userType = userType;
            this.phoneNumber = phoneNumber;
        }

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public UserType getUserType() { return userType; }
        public void setUserType(UserType userType) { this.userType = userType; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    public static class LinkSeniorRequest {
        private String guardianUserId;
        private String seniorPhoneNumber;

        // Getters and Setters
        public String getGuardianUserId() { return guardianUserId; }
        public void setGuardianUserId(String guardianUserId) { this.guardianUserId = guardianUserId; }
        public String getSeniorPhoneNumber() { return seniorPhoneNumber; }
        public void setSeniorPhoneNumber(String seniorPhoneNumber) { this.seniorPhoneNumber = seniorPhoneNumber; }
    }

    public static class LinkSeniorResponse {
        private boolean success;
        private String message;
        private Object data;

        public LinkSeniorResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class LinkedSeniorsResponse {
        private boolean success;
        private String message;
        private Object data;

        public LinkedSeniorsResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
