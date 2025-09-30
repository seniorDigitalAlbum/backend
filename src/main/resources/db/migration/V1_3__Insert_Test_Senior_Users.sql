-- 테스트용 시니어 사용자 3명 추가 (모든 컬럼 포함)
INSERT INTO users (kakao_id, nickname, profile_image_url, gender, phone_number, user_type, is_active, created_at, updated_at, last_login_at) VALUES
('test_senior_001', '김할머니', 'https://via.placeholder.com/150/FFB6C1/000000?text=김할머니', 'female', '010-1111-2222', 'SENIOR', true, '2024-01-15 09:30:00', '2024-01-15 09:30:00', '2024-01-15 14:20:00'),
('test_senior_002', '박할아버지', 'https://via.placeholder.com/150/87CEEB/000000?text=박할아버지', 'male', '010-3333-4444', 'SENIOR', true, '2024-01-16 10:15:00', '2024-01-16 10:15:00', '2024-01-16 16:45:00'),
('test_senior_003', '이할머니', 'https://via.placeholder.com/150/98FB98/000000?text=이할머니', 'female', '010-5555-6666', 'SENIOR', true, '2024-01-17 11:00:00', '2024-01-17 11:00:00', '2024-01-17 18:30:00');
