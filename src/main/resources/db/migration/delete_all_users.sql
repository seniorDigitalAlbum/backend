USE my_new_db;

-- 외래키 제약조건을 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 모든 테이블에서 사용자 관련 데이터 삭제
DELETE FROM album_comments;
DELETE FROM album_photos;
DELETE FROM albums;
DELETE FROM camera_sessions;
DELETE FROM conversation_messages;
DELETE FROM conversations;
DELETE FROM guardian_senior_relationships;
DELETE FROM media_files;
DELETE FROM microphone_sessions;
DELETE FROM music_recommendations;
DELETE FROM notifications;
DELETE FROM user_emotion_analysis;
DELETE FROM user_links;
DELETE FROM users;

-- 외래키 제약조건을 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 자동 증가 값 리셋
ALTER TABLE users AUTO_INCREMENT = 1;

-- 결과 확인
SELECT COUNT(*) as remaining_users FROM users;