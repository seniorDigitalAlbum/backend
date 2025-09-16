package com.chimaenono.dearmind.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 파일을 S3에 업로드하고 URL을 반환합니다.
     * 
     * @param file 업로드할 파일
     * @param folder S3 내 폴더 경로 (예: "diary-covers", "user-photos")
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 고유한 파일명 생성
            String fileName = generateFileName(file.getOriginalFilename());
            String key = folder + "/" + fileName;

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            // 업로드된 파일의 URL 생성 (리전 정보 포함)
            String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
            
            log.info("파일 업로드 성공: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     * 
     * @param fileUrl 삭제할 파일의 S3 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // URL에서 키 추출
            String key = extractKeyFromUrl(fileUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("파일 삭제 성공: {}", key);

        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 파일이 S3에 존재하는지 확인합니다.
     * 
     * @param fileUrl 확인할 파일의 S3 URL
     * @return 파일 존재 여부
     */
    public boolean fileExists(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * S3에서 파일을 다운로드합니다.
     * 
     * @param fileUrl 다운로드할 파일의 S3 URL
     * @return 파일의 바이트 배열
     */
    public byte[] downloadFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            byte[] fileBytes = s3Client.getObject(getObjectRequest).readAllBytes();
            log.info("파일 다운로드 성공: {}", key);
            return fileBytes;

        } catch (Exception e) {
            log.error("파일 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 다운로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 파일의 메타데이터를 가져옵니다.
     * 
     * @param fileUrl 파일의 S3 URL
     * @return 파일 메타데이터
     */
    public Map<String, Object> getFileMetadata(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("contentType", response.contentType());
            metadata.put("contentLength", response.contentLength());
            metadata.put("lastModified", response.lastModified());
            metadata.put("etag", response.eTag());
            
            log.info("파일 메타데이터 조회 성공: {}", key);
            return metadata;

        } catch (Exception e) {
            log.error("파일 메타데이터 조회 실패: {}", e.getMessage());
            throw new RuntimeException("파일 메타데이터 조회에 실패했습니다.", e);
        }
    }

    /**
     * 고유한 파일명을 생성합니다.
     * 
     * @param originalFilename 원본 파일명
     * @return 고유한 파일명
     */
    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return timestamp + "_" + uuid + extension;
    }

    /**
     * S3 URL에서 키를 추출합니다.
     * 
     * @param fileUrl S3 파일 URL
     * @return S3 키
     */
    private String extractKeyFromUrl(String fileUrl) {
        // URL 형식: https://bucket-name.s3.region.amazonaws.com/folder/filename
        String[] parts = fileUrl.split("/");
        if (parts.length >= 4) {
            StringBuilder key = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (i > 3) key.append("/");
                key.append(parts[i]);
            }
            return key.toString();
        }
        throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + fileUrl);
    }
}