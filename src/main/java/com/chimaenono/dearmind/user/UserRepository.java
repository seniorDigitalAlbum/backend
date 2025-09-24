package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUserId(String userId);
    
    List<User> findByUserType(User.UserType userType);
    
    boolean existsByUserId(String userId);
    
    Optional<User> findByPhone(String phone);
    
    boolean existsByPhone(String phone);
    
    Optional<User> findByKakaoId(String kakaoId);
    
    boolean existsByKakaoId(String kakaoId);
    
    List<User> findByPhoneInAndUserType(List<String> phoneNumbers, User.UserType userType);
    
    List<User> findByKakaoIdInAndUserType(List<String> kakaoIds, User.UserType userType);
}
