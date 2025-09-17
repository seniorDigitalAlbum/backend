package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUserId(String userId);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByUserType(User.UserType userType);
    
    boolean existsByEmail(String email);
    
    boolean existsByUserId(String userId);
    
    Optional<User> findByPhone(String phone);
    
    boolean existsByPhone(String phone);
}
