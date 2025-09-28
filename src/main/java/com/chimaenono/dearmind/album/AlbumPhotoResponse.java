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
@Schema(description = "앨범 사진 응답 DTO")
public class AlbumPhotoResponse {
    
    @Schema(description = "사진 ID", example = "1")
    private Long id;
    
    @Schema(description = "대화 ID", example = "123")
    private Long conversationId;
    
    @Schema(description = "이미지 URL", example = "https://s3.amazonaws.com/...")
    private String imageUrl;
    
    @Schema(description = "표지 사진 여부", example = "false")
    private Boolean isCover;
    
    @Schema(description = "작성자 ID", example = "456")
    private Long userId;
    
    @Schema(description = "작성자 닉네임", example = "김채현")
    private String authorNickname;
    
    @Schema(description = "작성자 프로필 이미지", example = "https://k.kakaocdn.net/...")
    private String authorProfileImage;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
    
    // AlbumPhoto 엔티티로부터 Response DTO 생성
    public static AlbumPhotoResponse from(AlbumPhoto photo, String authorNickname, String authorProfileImage) {
        return AlbumPhotoResponse.builder()
                .id(photo.getId())
                .conversationId(photo.getConversationId())
                .imageUrl(photo.getImageUrl())
                .isCover(photo.getIsCover())
                .userId(photo.getUserId())
                .authorNickname(authorNickname)
                .authorProfileImage(authorProfileImage)
                .createdAt(photo.getCreatedAt())
                .updatedAt(photo.getUpdatedAt())
                .build();
    }
}
