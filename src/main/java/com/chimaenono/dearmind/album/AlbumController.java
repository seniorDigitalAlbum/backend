package com.chimaenono.dearmind.album;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/albums")
@Tag(name = "Album API", description = "앨범 관리 API")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class AlbumController {
    
    @Autowired
    private AlbumService albumService;
    
    @PostMapping
    @Operation(summary = "앨범 생성", description = "새로운 앨범을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Album> createAlbum(
            @Parameter(description = "사용자 ID", example = "user123") @RequestParam String userId,
            @Parameter(description = "대화 세션 ID", example = "1") @RequestParam Long conversationId,
            @Parameter(description = "최종 감정", example = "기쁨") @RequestParam String finalEmotion,
            @Parameter(description = "일기 내용", example = "오늘은 어린 시절 추억에 대해 이야기하며 따뜻한 기분을 느꼈습니다.") @RequestParam String diaryContent) {
        
        Album album = albumService.createAlbum(userId, conversationId, finalEmotion, diaryContent);
        return ResponseEntity.ok(album);
    }
    
    @GetMapping("/{albumId}")
    @Operation(summary = "앨범 상세 조회", description = "앨범의 상세 정보(최종 감정, 일기 내용)를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 조회 성공"),
        @ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음")
    })
    public ResponseEntity<Album> getAlbum(
            @Parameter(description = "앨범 ID", example = "1") @PathVariable Long albumId) {
        
        Optional<Album> album = albumService.getAlbumById(albumId);
        return album.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 앨범 목록", description = "사용자의 모든 앨범 목록을 최신순으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 목록 조회 성공")
    })
    public ResponseEntity<List<Album>> getAlbumsByUser(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        List<Album> albums = albumService.getAlbumsByUserId(userId);
        return ResponseEntity.ok(albums);
    }
    
    @GetMapping("/user/{userId}/count")
    @Operation(summary = "사용자 앨범 개수", description = "사용자가 가진 앨범의 총 개수를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 개수 조회 성공")
    })
    public ResponseEntity<Long> getAlbumCount(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        long count = albumService.getAlbumCountByUserId(userId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "대화 세션별 앨범 조회", description = "대화 세션 ID로 앨범을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 조회 성공"),
        @ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음")
    })
    public ResponseEntity<Album> getAlbumByConversation(
            @Parameter(description = "대화 세션 ID", example = "1") @PathVariable Long conversationId) {
        
        Optional<Album> album = albumService.getAlbumByConversationId(conversationId);
        return album.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{albumId}")
    @Operation(summary = "앨범 업데이트", description = "앨범의 최종 감정과 일기 내용을 업데이트합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음")
    })
    public ResponseEntity<Album> updateAlbum(
            @Parameter(description = "앨범 ID", example = "1") @PathVariable Long albumId,
            @Parameter(description = "최종 감정", example = "기쁨") @RequestParam String finalEmotion,
            @Parameter(description = "일기 내용", example = "오늘은 어린 시절 추억에 대해 이야기하며 따뜻한 기분을 느꼈습니다.") @RequestParam String diaryContent) {
        
        Album album = albumService.updateAlbum(albumId, finalEmotion, diaryContent);
        if (album != null) {
            return ResponseEntity.ok(album);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{albumId}")
    @Operation(summary = "앨범 삭제", description = "앨범을 삭제합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음")
    })
    public ResponseEntity<String> deleteAlbum(
            @Parameter(description = "앨범 ID", example = "1") @PathVariable Long albumId) {
        
        boolean deleted = albumService.deleteAlbum(albumId);
        if (deleted) {
            return ResponseEntity.ok("앨범이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}/emotion/{emotion}")
    @Operation(summary = "감정별 앨범 조회", description = "사용자의 특정 감정 앨범들을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앨범 목록 조회 성공")
    })
    public ResponseEntity<List<Album>> getAlbumsByEmotion(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId,
            @Parameter(description = "감정", example = "기쁨") @PathVariable String emotion) {
        
        List<Album> albums = albumService.getAlbumsByEmotion(userId, emotion);
        return ResponseEntity.ok(albums);
    }
    
    @PostMapping("/dummy/{userId}")
    @Operation(summary = "더미 앨범 데이터 생성", description = "테스트용 더미 앨범 데이터를 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "더미 데이터 생성 성공")
    })
    public ResponseEntity<String> createDummyAlbums(
            @Parameter(description = "사용자 ID", example = "user123") @PathVariable String userId) {
        
        albumService.createDummyAlbums(userId);
        return ResponseEntity.ok("더미 앨범 데이터가 성공적으로 생성되었습니다.");
    }
    
    @GetMapping("/health")
    @Operation(summary = "앨범 서비스 상태 확인", description = "앨범 서비스의 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Album service is running");
    }
} 