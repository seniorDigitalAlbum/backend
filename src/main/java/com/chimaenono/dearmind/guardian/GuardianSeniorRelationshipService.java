package com.chimaenono.dearmind.guardian;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chimaenono.dearmind.user.User;
import com.chimaenono.dearmind.user.UserService;
import com.chimaenono.dearmind.notification.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GuardianSeniorRelationshipService {

    private final GuardianSeniorRelationshipRepository relationshipRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 보호자-시니어 관계 요청 생성
     * @param guardianId 보호자 ID
     * @param seniorId 시니어 ID
     * @return 생성된 관계 정보
     */
    @Transactional
    public GuardianSeniorRelationship createRelationship(Long guardianId, Long seniorId) {
        log.info("보호자-시니어 관계 요청 생성: guardianId={}, seniorId={}", guardianId, seniorId);

        // 사용자 존재 여부 및 타입 확인
        User guardian = userService.findById(guardianId)
                .orElseThrow(() -> new RuntimeException("보호자를 찾을 수 없습니다: " + guardianId));
        User senior = userService.findById(seniorId)
                .orElseThrow(() -> new RuntimeException("시니어를 찾을 수 없습니다: " + seniorId));

        // 사용자 타입 검증
        if (!"GUARDIAN".equals(guardian.getUserType())) {
            throw new RuntimeException("보호자 타입이 아닙니다: " + guardianId);
        }
        if (!"SENIOR".equals(senior.getUserType())) {
            throw new RuntimeException("시니어 타입이 아닙니다: " + seniorId);
        }

        // 이미 관계가 존재하는지 확인
        if (relationshipRepository.existsByGuardianAndSenior(guardianId, seniorId)) {
            throw new RuntimeException("이미 관계가 존재합니다");
        }

        // 관계 생성
        GuardianSeniorRelationship relationship = new GuardianSeniorRelationship(guardian, senior);
        GuardianSeniorRelationship savedRelationship = relationshipRepository.save(relationship);
        
        // 보호자에게 알람 생성
        try {
            notificationService.createGuardianRequestNotification(guardianId, seniorId, savedRelationship.getId());
            log.info("보호자 연결 요청 알람 생성 완료: guardianId={}, seniorId={}, relationshipId={}", 
                    guardianId, seniorId, savedRelationship.getId());
        } catch (Exception e) {
            log.error("보호자 연결 요청 알람 생성 실패: guardianId={}, seniorId={}, error={}", 
                    guardianId, seniorId, e.getMessage());
            // 알람 생성 실패해도 관계 생성은 성공으로 처리
        }
        
        return savedRelationship;
    }

    /**
     * 관계 승인
     * @param relationshipId 관계 ID
     * @param seniorId 시니어 ID (승인 권한 확인용)
     * @return 승인된 관계 정보
     */
    @Transactional
    public GuardianSeniorRelationship approveRelationship(Long relationshipId, Long seniorId) {
        log.info("관계 승인: relationshipId={}, seniorId={}", relationshipId, seniorId);

        GuardianSeniorRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new RuntimeException("관계를 찾을 수 없습니다: " + relationshipId));

        // 승인 권한 확인 (시니어만 승인 가능)
        if (!relationship.getSenior().getId().equals(seniorId)) {
            throw new RuntimeException("승인 권한이 없습니다");
        }

        // 상태 확인
        if (relationship.getStatus() != GuardianSeniorRelationship.RelationshipStatus.PENDING) {
            throw new RuntimeException("대기 중인 관계만 승인할 수 있습니다");
        }

        relationship.approve();
        GuardianSeniorRelationship savedRelationship = relationshipRepository.save(relationship);
        
        // 보호자에게 승인 알람 생성
        try {
            notificationService.createGuardianRequestApprovedNotification(
                    relationship.getGuardian().getId(), 
                    relationship.getSenior().getId(), 
                    savedRelationship.getId()
            );
            log.info("보호자 연결 요청 승인 알람 생성 완료: guardianId={}, seniorId={}, relationshipId={}", 
                    relationship.getGuardian().getId(), relationship.getSenior().getId(), savedRelationship.getId());
        } catch (Exception e) {
            log.error("보호자 연결 요청 승인 알람 생성 실패: guardianId={}, seniorId={}, error={}", 
                    relationship.getGuardian().getId(), relationship.getSenior().getId(), e.getMessage());
            // 알람 생성 실패해도 관계 승인은 성공으로 처리
        }
        
        return savedRelationship;
    }

    /**
     * 관계 거부
     * @param relationshipId 관계 ID
     * @param seniorId 시니어 ID (거부 권한 확인용)
     * @return 거부된 관계 정보
     */
    @Transactional
    public GuardianSeniorRelationship rejectRelationship(Long relationshipId, Long seniorId) {
        log.info("관계 거부: relationshipId={}, seniorId={}", relationshipId, seniorId);

        GuardianSeniorRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new RuntimeException("관계를 찾을 수 없습니다: " + relationshipId));

        // 거부 권한 확인 (시니어만 거부 가능)
        if (!relationship.getSenior().getId().equals(seniorId)) {
            throw new RuntimeException("거부 권한이 없습니다");
        }

        // 상태 확인
        if (relationship.getStatus() != GuardianSeniorRelationship.RelationshipStatus.PENDING) {
            throw new RuntimeException("대기 중인 관계만 거부할 수 있습니다");
        }

        relationship.reject();
        GuardianSeniorRelationship savedRelationship = relationshipRepository.save(relationship);
        
        // 보호자에게 거절 알람 생성
        try {
            notificationService.createGuardianRequestRejectedNotification(
                    relationship.getGuardian().getId(), 
                    relationship.getSenior().getId(), 
                    savedRelationship.getId()
            );
            log.info("보호자 연결 요청 거절 알람 생성 완료: guardianId={}, seniorId={}, relationshipId={}", 
                    relationship.getGuardian().getId(), relationship.getSenior().getId(), savedRelationship.getId());
        } catch (Exception e) {
            log.error("보호자 연결 요청 거절 알람 생성 실패: guardianId={}, seniorId={}, error={}", 
                    relationship.getGuardian().getId(), relationship.getSenior().getId(), e.getMessage());
            // 알람 생성 실패해도 관계 거절은 성공으로 처리
        }
        
        return savedRelationship;
    }

    /**
     * 보호자의 모든 관계 조회
     * @param guardianId 보호자 ID
     * @return 보호자의 관계 목록
     */
    public List<GuardianSeniorRelationship> getGuardianRelationships(Long guardianId) {
        log.info("보호자 관계 조회: guardianId={}", guardianId);
        return relationshipRepository.findByGuardianId(guardianId);
    }

    /**
     * 시니어의 모든 관계 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 관계 목록
     */
    public List<GuardianSeniorRelationship> getSeniorRelationships(Long seniorId) {
        log.info("시니어 관계 조회: seniorId={}", seniorId);
        return relationshipRepository.findBySeniorId(seniorId);
    }

    /**
     * 보호자의 승인된 관계만 조회
     * @param guardianId 보호자 ID
     * @return 보호자의 승인된 관계 목록
     */
    public List<GuardianSeniorRelationship> getApprovedGuardianRelationships(Long guardianId) {
        log.info("보호자 승인된 관계 조회: guardianId={}", guardianId);
        return relationshipRepository.findApprovedByGuardianId(guardianId);
    }

    /**
     * 시니어의 승인된 관계만 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 승인된 관계 목록
     */
    public List<GuardianSeniorRelationship> getApprovedSeniorRelationships(Long seniorId) {
        log.info("시니어 승인된 관계 조회: seniorId={}", seniorId);
        return relationshipRepository.findApprovedBySeniorId(seniorId);
    }

    /**
     * 시니어의 대기 중인 관계 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 대기 중인 관계 목록
     */
    public List<GuardianSeniorRelationship> getPendingSeniorRelationships(Long seniorId) {
        log.info("시니어 대기 중인 관계 조회: seniorId={}", seniorId);
        return relationshipRepository.findPendingBySeniorId(seniorId);
    }

    /**
     * 관계 삭제
     * @param relationshipId 관계 ID
     * @param userId 사용자 ID (삭제 권한 확인용)
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean deleteRelationship(Long relationshipId, Long userId) {
        log.info("관계 삭제: relationshipId={}, userId={}", relationshipId, userId);

        GuardianSeniorRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new RuntimeException("관계를 찾을 수 없습니다: " + relationshipId));

        // 삭제 권한 확인 (관계에 포함된 사용자만 삭제 가능)
        if (!relationship.getGuardian().getId().equals(userId) && 
            !relationship.getSenior().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다");
        }

        relationshipRepository.delete(relationship);
        return true;
    }

    /**
     * 관계 존재 여부 확인
     * @param guardianId 보호자 ID
     * @param seniorId 시니어 ID
     * @return 관계 존재 여부
     */
    public boolean relationshipExists(Long guardianId, Long seniorId) {
        return relationshipRepository.existsByGuardianAndSenior(guardianId, seniorId);
    }
}
