-- 외래키 제약조건을 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 모든 관계 삭제
DELETE FROM guardian_senior_relationships;

-- 외래키 제약조건을 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 결과 확인
SELECT COUNT(*) as remaining_relationships FROM guardian_senior_relationships;