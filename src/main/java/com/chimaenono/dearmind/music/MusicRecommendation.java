package com.chimaenono.dearmind.music;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "음악 추천 엔티티")
public class MusicRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "음악 추천 고유 ID", example = "1")
    private Long id;
    
    @Column(name = "conversation_id", nullable = false)
    @Schema(description = "대화 세션 ID", example = "123")
    private Long conversationId;
    
    @Column(name = "title", nullable = false)
    @Schema(description = "노래 제목", example = "너를 만나")
    private String title;
    
    @Column(name = "artist", nullable = false)
    @Schema(description = "가수 이름", example = "이승기")
    private String artist;
    
    @Column(name = "mood")
    @Schema(description = "음악 분위기", example = "따뜻하고 위로가 되는")
    private String mood;
    
    @Column(name = "youtube_link", columnDefinition = "TEXT")
    @Schema(description = "유튜브 링크", example = "https://www.youtube.com/watch?v=...")
    private String youtubeLink;
    
    @Column(name = "youtube_video_id")
    @Schema(description = "유튜브 비디오 ID", example = "dQw4w9WgXcQ")
    private String youtubeVideoId;
    
    @Column(name = "created_at", nullable = false)
    @Schema(description = "추천 생성 시간")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
