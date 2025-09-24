package com.chimaenono.dearmind.s3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
@Tag(name = "S3 파일 관리", description = "S3 파일 업로드, 삭제, 확인 API")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/upload")
    @Operation(summary = "파일 업로드", description = "S3에 파일을 업로드하고 URL을 반환합니다.")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder) {
        
        try {
            String fileUrl = s3Service.uploadFile(file, folder);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("fileUrl", fileUrl);
            response.put("message", "파일 업로드가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "파일 업로드에 실패했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("fileUrl") String fileUrl) {
        
        try {
            s3Service.deleteFile(fileUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "파일 삭제가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "파일 삭제에 실패했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/exists")
    @Operation(summary = "파일 존재 확인", description = "S3에 파일이 존재하는지 확인합니다.")
    public ResponseEntity<Map<String, Object>> fileExists(@RequestParam("fileUrl") String fileUrl) {
        
        try {
            boolean exists = s3Service.fileExists(fileUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", "true");
            response.put("exists", exists);
            response.put("message", exists ? "파일이 존재합니다." : "파일이 존재하지 않습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", "false");
            response.put("exists", false);
            response.put("message", "파일 존재 확인에 실패했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload/diary-cover")
    @Operation(summary = "일기장 표지 업로드", description = "일기장 표지 이미지를 업로드합니다.")
    public ResponseEntity<Map<String, String>> uploadDiaryCover(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "diary-covers");
    }

    @PostMapping("/upload/user-photo")
    @Operation(summary = "사용자 사진 업로드", description = "사용자 사진을 업로드합니다.")
    public ResponseEntity<Map<String, String>> uploadUserPhoto(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "user-photos");
    }

    @PostMapping("/upload/album-photo")
    @Operation(summary = "앨범 사진 업로드", description = "앨범 사진을 업로드합니다.")
    public ResponseEntity<Map<String, String>> uploadAlbumPhoto(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "album-photos");
    }

    @GetMapping("/download")
    @Operation(summary = "파일 다운로드", description = "S3에서 파일을 다운로드합니다.")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileUrl") String fileUrl) {
        
        try {
            byte[] fileBytes = s3Service.downloadFile(fileUrl);
            
            // 파일명 추출
            String fileName = extractFileNameFromUrl(fileUrl);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .body(fileBytes);
            
        } catch (Exception e) {
            log.error("파일 다운로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/metadata")
    @Operation(summary = "파일 메타데이터 조회", description = "S3 파일의 메타데이터를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@RequestParam("fileUrl") String fileUrl) {
        
        try {
            Map<String, Object> metadata = s3Service.getFileMetadata(fileUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", "true");
            response.put("metadata", metadata);
            response.put("message", "파일 메타데이터 조회가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 메타데이터 조회 실패: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "파일 메타데이터 조회에 실패했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * URL에서 파일명을 추출합니다.
     * 
     * @param fileUrl S3 파일 URL
     * @return 파일명
     */
    private String extractFileNameFromUrl(String fileUrl) {
        String[] parts = fileUrl.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "download";
    }
}