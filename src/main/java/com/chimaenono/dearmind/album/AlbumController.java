package com.chimaenono.dearmind.album;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.chimaenono.dearmind.s3.S3UploadService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/albums")
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
    public ResponseEntity<List<AlbumCommentResponse>> getComments(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId) {
        
        try {
            log.info("🔍 댓글 목록 조회 요청 시작: conversationId={}", conversationId);
            List<AlbumCommentResponse> comments = albumCommentService.getCommentsWithAuthorInfo(conversationId);
            log.info("✅ 댓글 목록 조회 성공: conversationId={}, 댓글 수={}", conversationId, comments.size());
            return ResponseEntity.ok(comments);
        } catch (IllegalArgumentException e) {
            log.error("❌ 댓글 목록 조회 실패 - 잘못된 요청: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ 댓글 목록 조회 실패 - 서버 오류: conversationId={}, error={}", conversationId, e.getMessage(), e);
            e.printStackTrace(); // 스택 트레이스 출력
            return ResponseEntity.internalServerError().build();
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
                    request.getUserId()
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
    public ResponseEntity<List<AlbumPhotoResponse>> getPhotos(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId) {
        
        try {
            log.info("🔍 사진 목록 조회 요청 시작: conversationId={}", conversationId);
            List<AlbumPhotoResponse> photos = albumPhotoService.getPhotosWithAuthorInfo(conversationId);
            log.info("✅ 사진 목록 조회 성공: conversationId={}, 사진 수={}", conversationId, photos.size());
            return ResponseEntity.ok(photos);
        } catch (IllegalArgumentException e) {
            log.error("❌ 사진 목록 조회 실패 - 잘못된 요청: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ 사진 목록 조회 실패 - 서버 오류: conversationId={}, error={}", conversationId, e.getMessage(), e);
            e.printStackTrace(); // 스택 트레이스 출력
            return ResponseEntity.internalServerError().build();
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
                    request.getUserId()
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
     * 파일을 직접 업로드하여 새로운 사진을 추가합니다.
     */
    @PostMapping("/{conversationId}/photos/upload")
    @Operation(summary = "사진 파일 업로드 및 추가", description = "파일을 직접 업로드하여 앨범에 사진을 추가합니다.")
    public ResponseEntity<AlbumPhoto> addPhotoWithUpload(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal(expression = "id") Long userId) {
        
        try {
            // 1. S3에 파일 업로드
            String imageUrl = s3UploadService.uploadImage(file, "album-photos");
            
            // 2. 앨범에 사진 추가
            AlbumPhoto photo = albumPhotoService.addPhoto(conversationId, imageUrl, userId);
            
            return ResponseEntity.ok(photo);
        } catch (IllegalArgumentException e) {
            log.error("사진 업로드 및 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("사진 업로드 및 추가 실패: conversationId={}, error={}", conversationId, e.getMessage());
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

    // ========== 앨범 공개 상태 관리 API ==========

    /**
     * 앨범 공개 상태를 업데이트합니다.
     */
    @PutMapping("/{conversationId}/visibility")
    @Operation(summary = "앨범 공개 상태 업데이트", description = "앨범의 공개/비공개 상태를 업데이트합니다.")
    public ResponseEntity<Map<String, Object>> updateAlbumVisibility(
            @Parameter(description = "대화 ID", required = true)
            @PathVariable Long conversationId,
            @RequestBody Map<String, Boolean> request) {
        
        try {
            log.info("🔍 앨범 공개 상태 업데이트 요청: conversationId={}, isPublic={}", conversationId, request.get("isPublic"));
            
            Boolean isPublic = request.get("isPublic");
            if (isPublic == null) {
                log.error("❌ 앨범 공개 상태 업데이트 실패 - isPublic 파라미터 누락");
                return ResponseEntity.badRequest().build();
            }
            
            // 실제 데이터베이스 업데이트
            albumPhotoService.updateAlbumVisibility(conversationId, isPublic);
            log.info("✅ 앨범 공개 상태 업데이트 완료: conversationId={}, isPublic={}", conversationId, isPublic);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isPublic", isPublic);
            response.put("message", isPublic ? "앨범이 가족에게 공개되었습니다." : "앨범이 가족에게 비공개로 설정되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 앨범 공개 상태 업데이트 실패: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/senior/{seniorUserId}/cover-photo")
    @Operation(summary = "시니어 표지 사진 조회", description = "시니어의 최신 표지 사진을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getSeniorCoverPhoto(
            @Parameter(description = "시니어 사용자 ID", required = true)
            @PathVariable String seniorUserId) {
        
        try {
            log.info("🔍 시니어 표지 사진 조회 요청: seniorUserId={}", seniorUserId);
            
            // 시니어의 최신 표지 사진 조회
            AlbumPhoto coverPhoto = albumPhotoService.getSeniorCoverPhoto(seniorUserId);
            
            if (coverPhoto == null) {
                log.info("🔍 시니어 표지 사진 없음: seniorUserId={}", seniorUserId);
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("imageUrl", coverPhoto.getImageUrl());
            response.put("conversationId", coverPhoto.getConversationId());
            response.put("createdAt", coverPhoto.getCreatedAt());
            
            log.info("✅ 시니어 표지 사진 조회 성공: seniorUserId={}, imageUrl={}", seniorUserId, coverPhoto.getImageUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 시니어 표지 사진 조회 실패: seniorUserId={}, error={}", seniorUserId, e.getMessage(), e);
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
        private Long userId;

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class PhotoRequest {
        private String imageUrl;
        private Long userId;

        // Getters and Setters
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
