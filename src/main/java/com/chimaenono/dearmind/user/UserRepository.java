package com.chimaenono.dearmind.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 카카오 ID로 사용자 조회
     * @param kakaoId 카카오 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByKakaoId(String kakaoId);
    
    /**
     * 카카오 ID로 사용자 존재 여부 확인
     * @param kakaoId 카카오 사용자 ID
     * @return 존재 여부
     */
    boolean existsByKakaoId(String kakaoId);
    
    /**
     * 닉네임으로 사용자 조회
     * @param nickname 닉네임
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByNickname(String nickname);
    
    /**
     * 닉네임 중복 확인
     * @param nickname 닉네임
     * @return 중복 여부
     */
    boolean existsByNickname(String nickname);
    
    /**
     * 활성화된 사용자만 조회
     * @param kakaoId 카카오 사용자 ID
     * @return 활성화된 사용자 정보 (Optional)
     */
    @Query("SELECT u FROM User u WHERE u.kakaoId = :kakaoId AND u.isActive = true")
    Optional<User> findActiveUserByKakaoId(@Param("kakaoId") String kakaoId);
    
    /**
     * 이름으로 시니어 검색 (userType이 SENIOR인 사용자만)
     * @param name 검색할 이름
     * @return 시니어 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SENIOR' AND u.nickname LIKE %:name% AND u.isActive = true")
    List<User> findSeniorsByName(@Param("name") String name);
    
    /**
     * 전화번호로 시니어 검색 (userType이 SENIOR인 사용자만)
     * @param phoneNumber 검색할 전화번호
     * @return 시니어 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SENIOR' AND u.phoneNumber LIKE %:phoneNumber% AND u.isActive = true")
    List<User> findSeniorsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    /**
     * 이름 또는 전화번호로 시니어 통합 검색 (userType이 SENIOR인 사용자만)
     * @param searchTerm 검색어 (이름 또는 전화번호)
     * @return 시니어 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SENIOR' AND u.isActive = true AND (u.nickname LIKE %:searchTerm% OR u.phoneNumber LIKE %:searchTerm%)")
    List<User> findSeniorsByNameOrPhoneNumber(@Param("searchTerm") String searchTerm);
    
    /**
     * 이름과 전화번호 모두 일치하는 시니어 검색 (userType이 SENIOR인 사용자만)
     * @param name 검색할 이름
     * @param phoneNumber 검색할 전화번호
     * @return 시니어 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SENIOR' AND u.isActive = true AND u.nickname = :name AND u.phoneNumber = :phoneNumber")
    List<User> findSeniorsByNameAndPhoneNumber(@Param("name") String name, @Param("phoneNumber") String phoneNumber);
    
}
