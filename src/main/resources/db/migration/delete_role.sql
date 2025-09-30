-- 특정 사용자 이름의 역할 삭제
DELETE FROM users 
WHERE nickname = '나림';

-- 삭제 전 확인
SELECT * FROM users WHERE nickname = '나림';

-- 삭제 후 확인
SELECT COUNT(*) as remaining_users FROM users;