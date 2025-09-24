package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkRepository extends JpaRepository<UserLink, Long> {
    
    List<UserLink> findByGuardianId(Long guardianId);
    
    List<UserLink> findBySeniorId(Long seniorId);
    
    List<UserLink> findByStatus(String status);
    
    boolean existsByGuardianIdAndSeniorId(Long guardianId, Long seniorId);
    
    Optional<UserLink> findByGuardianIdAndSeniorId(Long guardianId, Long seniorId);
}
