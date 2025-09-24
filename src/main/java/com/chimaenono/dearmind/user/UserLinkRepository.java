package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserLinkRepository extends JpaRepository<UserLink, Long> {
    
    Optional<UserLink> findByGuardianIdAndSeniorId(Long guardianId, Long seniorId);
    
    List<UserLink> findByGuardianId(Long guardianId);
    
    List<UserLink> findBySeniorId(Long seniorId);
    
    boolean existsByGuardianIdAndSeniorId(Long guardianId, Long seniorId);
}