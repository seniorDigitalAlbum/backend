package com.chimaenono.dearmind.guardian;

import com.chimaenono.dearmind.kakao.dto.KakaoFriendsResponse;
import com.chimaenono.dearmind.kakao.service.KakaoAuthService;
import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/guardian")
@RequiredArgsConstructor
@Tag(name = "보호자 관리", description = "보호자-시니어 연결 관련 API")
public class GuardianController {

    private final UserService userService;
    private final KakaoAuthService kakaoAuthService;

    @PostMapping("/search-kakao-friends")
    @Operation(summary = "카카오 친구 중 시니어 검색", description = "카카오 친구 목록에서 우리 앱에 가입된 시니어를 검색합니다.")
    public ResponseEntity<List<SeniorInfo>> searchKakaoFriends(@RequestBody SearchKakaoFriendsRequest request) {
        try {
            log.info("카카오 친구 중 시니어 검색 요청");
            
            // 1. 카카오 친구 목록 조회
            KakaoFriendsResponse friendsResponse = kakaoAuthService.getFriends(request.getAccessToken());
            
            if (friendsResponse == null || friendsResponse.getElements() == null) {
                log.warn("카카오 친구 목록이 비어있습니다.");
                return ResponseEntity.ok(List.of());
            }
            
            // 2. 친구들의 카카오 ID 추출
            List<Long> kakaoIds = friendsResponse.getElements().stream()
                .map(KakaoFriendsResponse.KakaoFriend::getId)
                .collect(Collectors.toList());
            
            // 3. 우리 앱에 가입된 시니어 중 카카오 친구인 사용자 검색
            List<User> seniors = userService.findByKakaoIdsAndUserType(kakaoIds, "SENIOR");
            
            // 4. 카카오 친구 정보와 앱 사용자 정보 매핑
            List<SeniorInfo> seniorInfos = seniors.stream()
                .map(user -> {
                    KakaoFriendsResponse.KakaoFriend kakaoFriend = friendsResponse.getElements().stream()
                        .filter(friend -> friend.getId().equals(Long.parseLong(user.getKakaoId())))
                        .findFirst()
                        .orElse(null);
                    
                    return SeniorInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .profileImage(user.getProfileImage())
                        .kakaoId(user.getKakaoId())
                        .kakaoNickname(kakaoFriend != null ? kakaoFriend.getProfileNickname() : user.getName())
                        .kakaoProfileImage(kakaoFriend != null ? kakaoFriend.getProfileThumbnailImage() : user.getProfileImage())
                        .build();
                })
                .collect(Collectors.toList());
            
            log.info("카카오 친구 중 검색된 시니어 수: {}", seniorInfos.size());
            return ResponseEntity.ok(seniorInfos);
            
        } catch (Exception e) {
            log.error("카카오 친구 중 시니어 검색 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/connect-senior")
    @Operation(summary = "시니어와 연결", description = "보호자와 시니어를 연결합니다.")
    public ResponseEntity<ConnectResponse> connectSenior(@RequestBody ConnectRequest request) {
        try {
            log.info("시니어 연결 요청: 보호자={}, 시니어={}", request.getGuardianId(), request.getSeniorId());
            
            boolean success = userService.connectGuardianAndSenior(
                request.getGuardianId(), 
                request.getSeniorId()
            );
            
            if (success) {
                log.info("시니어 연결 성공");
                return ResponseEntity.ok(ConnectResponse.builder()
                    .success(true)
                    .message("시니어와 연결되었습니다.")
                    .build());
            } else {
                log.warn("시니어 연결 실패");
                return ResponseEntity.ok(ConnectResponse.builder()
                    .success(false)
                    .message("연결에 실패했습니다.")
                    .build());
            }
            
        } catch (Exception e) {
            log.error("시니어 연결 처리 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/connected-seniors/{guardianId}")
    @Operation(summary = "연결된 시니어 목록 조회", description = "보호자와 연결된 시니어 목록을 조회합니다.")
    public ResponseEntity<List<SeniorInfo>> getConnectedSeniors(@PathVariable Long guardianId) {
        try {
            log.info("연결된 시니어 목록 조회 요청: guardianId={}", guardianId);
            
            List<User> connectedSeniors = userService.getConnectedSeniorsByGuardianId(guardianId);
            
            List<SeniorInfo> seniorInfos = connectedSeniors.stream()
                .map(user -> SeniorInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .profileImage(user.getProfileImage())
                    .kakaoId(user.getKakaoId())
                    .build())
                .collect(Collectors.toList());
            
            log.info("연결된 시니어 수: {}", seniorInfos.size());
            return ResponseEntity.ok(seniorInfos);
            
        } catch (Exception e) {
            log.error("연결된 시니어 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
