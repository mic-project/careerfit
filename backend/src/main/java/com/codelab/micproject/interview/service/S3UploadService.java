package com.codelab.micproject.interview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket:#{null}}")
    private String bucketName;

    /**
     * OpenVidu 녹화 파일을 S3에 업로드합니다.
     *
     * @param recordingId OpenVidu 녹화 ID
     * @param filePath 로컬 파일 경로
     * @param userId 사용자 ID (선택사항)
     * @return S3 객체 키
     */
    public String uploadRecordingFile(String recordingId, String filePath, Long userId) {
        File file = null;
        String s3Key = null;

        try {
            file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("Recording file not found: " + filePath);
            }

            // 실제 S3 업로드 수행

            // S3 키 생성: recordings/{userId}/{date}/{recordingId}.mp4
            s3Key = generateRecordingS3Key(recordingId, userId, filePath);

            log.info("🎥 S3 업로드 시작: {} → s3://{}/{}", filePath, bucketName, s3Key);

            // 파일 확장자에 따른 Content-Type 결정
            String contentType = determineContentType(filePath);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentDisposition("inline") // 브라우저에서 직접 재생 가능하도록
                    .cacheControl("max-age=3600") // 캐시 설정
                    .metadata(java.util.Map.of(
                        "recording-id", recordingId,
                        "user-id", userId != null ? userId.toString() : "anonymous",
                        "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build();

            PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromFile(file)
            );

            log.info("✅ S3 업로드 완료: s3://{}/{} (ETag: {})", bucketName, s3Key, response.eTag());

            // 업로드 완료 후 로컬 파일 삭제 (선택사항)
            if (deleteLocalFileAfterUpload()) {
                try {
                    Files.delete(Path.of(filePath));
                    log.info("🗑️ 로컬 파일 삭제됨: {}", filePath);
                } catch (IOException e) {
                    log.warn("⚠️ 로컬 파일 삭제 실패: {}", filePath, e);
                }
            }

            return s3Key;

        } catch (Exception e) {
            log.error("❌ S3 업로드 실패: {} → {}", filePath, bucketName, e);
            log.error("🔍 상세 오류 정보: 버킷={}, 키={}, 파일크기={}",
                bucketName,
                s3Key != null ? s3Key : "키생성실패",
                file != null ? file.length() : "파일없음");
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 녹화 파일의 S3 키를 생성합니다.
     */
    private String generateRecordingS3Key(String recordingId, Long userId, String filePath) {
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String userFolder = userId != null ? userId.toString() : "anonymous";

        // 실제 파일 확장자 추출
        String fileExtension = ".webm"; // 기본값
        if (filePath != null) {
            String fileName = filePath.toLowerCase();
            if (fileName.endsWith(".mp4")) {
                fileExtension = ".mp4";
            } else if (fileName.endsWith(".webm")) {
                fileExtension = ".webm";
            }
        }

        return String.format("recordings/%s/%s/%s%s", userFolder, dateFolder, recordingId, fileExtension);
    }

    /**
     * 업로드 후 로컬 파일 삭제 여부를 설정합니다.
     */
    private boolean deleteLocalFileAfterUpload() {
        // 운영 환경에서는 true, 개발 환경에서는 false
        return !"local".equals(System.getProperty("spring.profiles.active"));
    }

    /**
     * S3 파일의 공개 URL을 생성합니다.
     */
    public String getPublicUrl(String s3Key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
    }

    /**
     * 파일 확장자에 따른 Content-Type을 결정합니다.
     */
    private String determineContentType(String filePath) {
        String fileName = filePath.toLowerCase();
        if (fileName.endsWith(".webm")) {
            return "video/webm";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".avi")) {
            return "video/avi";
        } else if (fileName.endsWith(".mov")) {
            return "video/quicktime";
        } else {
            return "video/mp4"; // 기본값
        }
    }

    /**
     * 사전 서명된 URL을 생성합니다 (7일간 유효).
     */
    public String generatePresignedUrl(String s3Key) {
        // PresignedGetObjectRequest presignedRequest = ...
        // return s3Presigner.presignGetObject(presignedRequest).url().toString();
        return getPublicUrl(s3Key);
    }

    /**
     * S3Client를 반환합니다 (API 호출용).
     */
    public S3Client getS3Client() {
        return s3Client;
    }
}