package com.chimaenono.dearmind.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Service s3Service;

    /**
     * 이미지를 S3에 업로드하고 URL을 반환합니다.
     * 
     * @param file 업로드할 이미지 파일
     * @param folder S3 내 폴더 경로 (예: "albums")
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadImage(MultipartFile file, String folder) {
        log.info("이미지 업로드 시작: 파일명={}, 크기={}, 폴더={}", 
                file.getOriginalFilename(), file.getSize(), folder);

        // 파일 유효성 검사
        validateImageFile(file);

        // S3에 업로드
        String imageUrl = s3Service.uploadFile(file, folder);
        
        log.info("이미지 업로드 완료: URL={}", imageUrl);
        return imageUrl;
    }

    /**
     * 이미지 파일의 유효성을 검사합니다.
     * 
     * @param file 검사할 파일
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 제한 (10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.");
        }

        // 파일 타입 검사
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 지원하는 이미지 형식 확인
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        boolean isAllowedType = false;
        for (String allowedType : allowedTypes) {
            if (contentType.equals(allowedType)) {
                isAllowedType = true;
                break;
            }
        }

        if (!isAllowedType) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. JPEG, PNG, GIF, WebP만 지원됩니다.");
        }
    }
}
