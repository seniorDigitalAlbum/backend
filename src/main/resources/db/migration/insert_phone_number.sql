-- 김춘자 사용자의 전화번호를 010-0000-0000으로 업데이트
UPDATE users 
SET phone_number = '010-0000-0000', 
    updated_at = NOW()
WHERE nickname = '김춘자';