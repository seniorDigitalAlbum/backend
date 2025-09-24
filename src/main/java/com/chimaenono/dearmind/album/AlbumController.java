package com.chimaenono.dearmind.album;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Album", description = "앨범 관련 API")
public class AlbumController {

    private final AlbumCommentService albumCommentService;
    private final AlbumPhotoService albumPhotoService;
    private final S3UploadService s3UploadService;

    // ========== 댓글 관련 API ==========

    /**
     * 특정 대화의 댓글 목록을 조회합니다.
     */
    @GetMapping("/{conversationId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "특정 대화의 댓글 목록을 조회합니다.")
    public ResponseEntity<List<AlbumComment>> getComments(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId) {
        
        try {
            List<AlbumComment> comments = albumCommentService.getCommentsByConversationId(conversationId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("댓글 목록 조회 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 새로운 댓글을 추가합니다.
     */
    @PostMapping("/{conversationId}/comments")
    @Operation(summary = "댓글 추가", description = "특정 대화에 새로운 댓글을 추가합니다.")
    public ResponseEntity<AlbumComment> addComment(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId,
            @RequestBody CommentRequest request) {
        
        try {
            AlbumComment comment = albumCommentService.addComment(
                    conversationId, 
                    request.getContent(), 
                    request.getAuthor()
            );
            return ResponseEntity.ok(comment);
        } catch (IllegalArgumentException e) {
            log.error("댓글 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("댓글 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 댓글을 삭제합니다.
     */
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "댓글 ID", required = true)
            @PathVariable Long commentId) {
        
        try {
            albumCommentService.deleteComment(commentId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("댓글 삭제 실패: commentId={}, error={}", commentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("댓글 삭제 실패: commentId={}, error={}", commentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== 사진 관련 API ==========

    /**
     * 특정 대화의 사진 목록을 조회합니다.
     */
    @GetMapping("/{conversationId}/photos")
    @Operation(summary = "사진 목록 조회", description = "특정 대화의 사진 목록을 조회합니다.")
    public ResponseEntity<List<AlbumPhoto>> getPhotos(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId) {
        
        try {
            List<AlbumPhoto> photos = albumPhotoService.getPhotosByConversationId(conversationId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            log.error("사진 목록 조회 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 특정 대화의 앨범 표지 사진을 조회합니다.
     */
    @GetMapping("/{conversationId}/photos/cover")
    @Operation(summary = "앨범 표지 사진 조회", description = "특정 대화의 앨범 표지 사진을 조회합니다.")
    public ResponseEntity<AlbumPhoto> getCoverPhoto(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId) {
        
        try {
            return albumPhotoService.getCoverPhotoByConversationId(conversationId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("앨범 표지 사진 조회 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 새로운 사진을 추가합니다.
     */
    @PostMapping("/{conversationId}/photos")
    @Operation(summary = "사진 추가", description = "특정 대화에 새로운 사진을 추가합니다.")
    public ResponseEntity<AlbumPhoto> addPhoto(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId,
            @RequestBody PhotoRequest request) {
        
        try {
            AlbumPhoto photo = albumPhotoService.addPhoto(
                    conversationId, 
                    request.getImageUrl(), 
                    request.getUploadedBy()
            );
            return ResponseEntity.ok(photo);
        } catch (IllegalArgumentException e) {
            log.error("사진 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("사진 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 사진을 앨범 표지로 설정합니다.
     */
    @PutMapping("/{conversationId}/photos/{photoId}/set-cover")
    @Operation(summary = "앨범 표지 설정", description = "특정 사진을 앨범 표지로 설정합니다.")
    public ResponseEntity<Void> setCoverPhoto(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId,
            @Parameter(description = "사진 ID", required = true)
            @PathVariable Long photoId) {
        
        try {
            albumPhotoService.setCoverPhoto(conversationId, photoId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("앨범 표지 설정 실패: conversationId={}, photoId={}, error={}", 
                    conversationId, photoId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("앨범 표지 설정 실패: conversationId={}, photoId={}, error={}", 
                    conversationId, photoId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사진을 삭제합니다.
     */
    @DeleteMapping("/photos/{photoId}")
    @Operation(summary = "사진 삭제", description = "특정 사진을 삭제합니다.")
    public ResponseEntity<Void> deletePhoto(
            @Parameter(description = "사진 ID", required = true)
            @PathVariable Long photoId) {
        
        try {
            albumPhotoService.deletePhoto(photoId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("사진 삭제 실패: photoId={}, error={}", photoId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("사진 삭제 실패: photoId={}, error={}", photoId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== S3 이미지 업로드 API ==========

    /**
     * 이미지를 S3에 업로드합니다.
     */
    @PostMapping("/upload/image")
    @Operation(summary = "이미지 업로드", description = "이미지를 S3에 업로드하고 URL을 반환합니다.")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("image") MultipartFile file) {
        
        try {
            String imageUrl = s3UploadService.uploadImage(file, "albums");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "이미지 업로드 성공");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("이미지 업로드 실패: error={}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "이미지 업로드 실패: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ========== DTO 클래스 ==========

    public static class CommentRequest {
        private String content;
        private String author = "가족";

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    public static class PhotoRequest {
        private String imageUrl;
        private String uploadedBy = "가족";

        // Getters and Setters
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    }
}
