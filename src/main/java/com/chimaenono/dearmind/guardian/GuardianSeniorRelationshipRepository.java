package com.chimaenono.dearmind.guardian;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianSeniorRelationshipRepository extends JpaRepository<GuardianSeniorRelationship, Long> {

    /**
     * 보호자와 시니어의 관계 조회
     * @param guardianId 보호자 ID
     * @param seniorId 시니어 ID
     * @return 관계 정보 (Optional)
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.guardian.id = :guardianId AND r.senior.id = :seniorId")
    Optional<GuardianSeniorRelationship> findByGuardianAndSenior(@Param("guardianId") Long guardianId, @Param("seniorId") Long seniorId);

    /**
     * 보호자의 모든 관계 조회
     * @param guardianId 보호자 ID
     * @return 보호자의 관계 목록
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.guardian.id = :guardianId")
    List<GuardianSeniorRelationship> findByGuardianId(@Param("guardianId") Long guardianId);

    /**
     * 시니어의 모든 관계 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 관계 목록
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.senior.id = :seniorId")
    List<GuardianSeniorRelationship> findBySeniorId(@Param("seniorId") Long seniorId);

    /**
     * 보호자의 승인된 관계만 조회
     * @param guardianId 보호자 ID
     * @return 보호자의 승인된 관계 목록
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.guardian.id = :guardianId AND r.status = 'APPROVED'")
    List<GuardianSeniorRelationship> findApprovedByGuardianId(@Param("guardianId") Long guardianId);

    /**
     * 시니어의 승인된 관계만 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 승인된 관계 목록
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.senior.id = :guardianId AND r.status = 'APPROVED'")
    List<GuardianSeniorRelationship> findApprovedBySeniorId(@Param("seniorId") Long seniorId);

    /**
     * 시니어의 대기 중인 관계 조회
     * @param seniorId 시니어 ID
     * @return 시니어의 대기 중인 관계 목록
     */
    @Query("SELECT r FROM GuardianSeniorRelationship r WHERE r.senior.id = :seniorId AND r.status = 'PENDING'")
    List<GuardianSeniorRelationship> findPendingBySeniorId(@Param("seniorId") Long seniorId);

    /**
     * 관계 존재 여부 확인
     * @param guardianId 보호자 ID
     * @param seniorId 시니어 ID
     * @return 관계 존재 여부
     */
    @Query("SELECT COUNT(r) > 0 FROM GuardianSeniorRelationship r WHERE r.guardian.id = :guardianId AND r.senior.id = :seniorId")
    boolean existsByGuardianAndSenior(@Param("guardianId") Long guardianId, @Param("seniorId") Long seniorId);
}
