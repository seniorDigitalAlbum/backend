package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkRepository extends JpaRepository<UserLink, Long> {
    
    Optional<UserLink> findByGuardianUserIdAndSeniorUserId(String guardianUserId, String seniorUserId);
    
    List<UserLink> findByGuardianUserId(String guardianUserId);
    
    List<UserLink> findBySeniorUserId(String seniorUserId);
    
    List<UserLink> findByStatus(String status);
    
    boolean existsByGuardianUserIdAndSeniorUserId(String guardianUserId, String seniorUserId);
}
