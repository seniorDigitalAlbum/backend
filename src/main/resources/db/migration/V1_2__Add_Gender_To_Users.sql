-- users 테이블에 성별 컬럼 추가
ALTER TABLE users ADD COLUMN gender ENUM('MALE', 'FEMALE') NULL AFTER phone;

-- 기존 사용자들의 성별을 NULL로 설정 (선택사항)
-- UPDATE users SET gender = NULL WHERE gender IS NULL;
