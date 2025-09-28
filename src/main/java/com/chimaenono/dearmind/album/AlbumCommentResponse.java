package com.chimaenono.dearmind.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "앨범 댓글 응답 DTO")
public class AlbumCommentResponse {
    
    @Schema(description = "댓글 ID", example = "1")
    private Long id;
    
    @Schema(description = "대화 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "작성자 ID", example = "456")
    private Long userId;
    
    @Schema(description = "댓글 내용", example = "정말 좋은 사진이네요!")
    private String content;
    
    @Schema(description = "작성자 닉네임", example = "김채현")
    private String authorNickname;
    
    @Schema(description = "작성자 프로필 이미지", example = "https://k.kakaocdn.net/...")
    private String authorProfileImage;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
    
    // AlbumComment 엔티티로부터 Response DTO 생성
    public static AlbumCommentResponse from(AlbumComment comment, String authorNickname, String authorProfileImage) {
        return AlbumCommentResponse.builder()
                .id(comment.getId())
                .conversationId(comment.getConversationId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .authorNickname(authorNickname)
                .authorProfileImage(authorProfileImage)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
