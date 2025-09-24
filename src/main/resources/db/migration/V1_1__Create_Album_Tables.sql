-- 앨범 댓글 테이블 생성
CREATE TABLE album_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    author VARCHAR(50) NOT NULL DEFAULT '가족',
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_album_comments_conversation_id (conversation_id),
    INDEX idx_album_comments_created_at (created_at),
    INDEX idx_album_comments_author (author)
);

-- 앨범 사진 테이블 생성
CREATE TABLE album_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by VARCHAR(50) NOT NULL DEFAULT '가족',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_album_photos_conversation_id (conversation_id),
    INDEX idx_album_photos_is_cover (is_cover),
    INDEX idx_album_photos_created_at (created_at),
    INDEX idx_album_photos_uploaded_by (uploaded_by),
    UNIQUE KEY uk_album_photos_conversation_cover (conversation_id, is_cover) -- 한 대화당 하나의 표지만 허용
);

-- 앨범 사진 테이블에 체크 제약조건 추가 (is_cover가 TRUE인 경우에만 UNIQUE 제약조건 적용)
-- MySQL에서는 부분적 유니크 인덱스를 직접 지원하지 않으므로, 애플리케이션 레벨에서 처리
