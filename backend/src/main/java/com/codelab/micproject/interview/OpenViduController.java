package com.codelab.micproject.interview;

import com.codelab.micproject.interview.service.S3UploadService;
import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/openvidu")
@RequiredArgsConstructor
public class OpenViduController {

    @Value("${openvidu.url}")
    private String openviduUrl;
    @Value("${openvidu.secret}")
    private String openviduSecret;
    private OpenVidu openvidu;

    private final S3UploadService s3UploadService;
    private final InterviewPracticeVideoRepository practiceVideoRepository;

    @PostConstruct
    public void init() {
        log.info("🚀 OpenVidu 초기화 시작...");
        log.info("📍 OpenVidu URL: {}", openviduUrl);
        log.info("🔑 OpenVidu Secret: {}", (openviduSecret != null ? "설정됨 (길이: " + openviduSecret.length() + ")" : "설정안됨"));

        try {
            TrustStrategy trustStrategy = (chain, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, trustStrategy).build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                    .setConnectionManager(cm);

            this.openvidu = new OpenVidu(openviduUrl, openviduSecret, httpClientBuilder);
            log.info("✅ OpenVidu 클라이언트 초기화 완료!");

        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("❌ OpenVidu 클라이언트 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("OpenVidu 클라이언트를 위한 커스텀 HttpClient 생성에 실패했습니다.", e);
        }
    }


    @PostMapping("/sessions")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params) {
        try {
            log.debug(">>> 세션 생성 시도: {}", params);
            SessionProperties properties = SessionProperties.fromJson(params).build();
            Session session = openvidu.createSession(properties);
            log.info("✅ 세션 생성 성공: {}", session.getSessionId());
            return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ 세션 생성 중 심각한 오류 발생", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params) {
        try {
            log.info("🔗 토큰 생성 시도 - 세션 ID: {}", sessionId);
            log.debug("📝 연결 파라미터: {}", params);

            Session session = openvidu.getActiveSession(sessionId);
            if (session == null) {
                log.error("❌ 세션을 찾을 수 없음: {}", sessionId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            ConnectionProperties properties = ConnectionProperties.fromJson(params).build();
            Connection connection = session.createConnection(properties);

            log.info("✅ 토큰 생성 성공!");
            log.debug("🎫 생성된 토큰: {}...", connection.getToken().substring(0, 50));

            return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ 토큰 생성 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/webhook")
    public ResponseEntity<Void> recordingWebhook(@RequestBody(required = false) Map<String, Object> params) {
        // ... (이하 동일) ...
        if (params == null || !params.containsKey("event")) {
            return ResponseEntity.ok().build();
        }

        String event = (String) params.get("event");
        if (!"recordingStatusChanged".equals(event)) {
            return ResponseEntity.ok().build();
        }

        String status = (String) params.get("status");
        if (!"ready".equals(status)) {
            return ResponseEntity.ok().build();
        }

        String recordingId = (String) params.get("id");
        String downloadUrl = (String) params.get("url");

        if (downloadUrl == null || downloadUrl.isBlank()) {
            log.error("Webhook 'ready' status received but no download URL was provided for recording {}", recordingId);
            return ResponseEntity.ok().build();
        }

        File tempFile = null;
        try {
            log.info("🎥 녹화 파일 다운로드 및 S3 업로드 시작: {}", recordingId);

            // OpenVidu에서 녹화 파일 다운로드
            URL url = new URL(downloadUrl);
            tempFile = Files.createTempFile("recording-" + recordingId + "-", ".mp4").toFile();
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("✅ 녹화 파일 다운로드 완료: {} (크기: {} bytes)", tempFile.getAbsolutePath(), tempFile.length());

            // S3에 업로드 (사용자 ID는 추후 세션 매핑에서 가져올 예정)
            String s3Key = s3UploadService.uploadRecordingFile(recordingId, tempFile.getAbsolutePath(), null);
            String publicUrl = s3UploadService.getPublicUrl(s3Key);

            // 데이터베이스에 녹화 정보 저장 (일시적으로 주석 처리)
            // PracticeVideo practiceVideo = new PracticeVideo(publicUrl);
            // practiceVideoRepository.save(practiceVideo);

            log.info("🎉 녹화 파일이 성공적으로 S3에 업로드되었습니다: {}", publicUrl);

        } catch (Exception e) {
            log.error("❌ 녹화 파일 처리 중 오류 발생: {}", e.getMessage(), e);
        } finally {
            // 임시 파일 정리 (S3UploadService에서도 처리하지만 보험용)
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    log.debug("🗑️ 임시 녹화 파일 삭제 완료: {}", tempFile.getAbsolutePath());
                } catch (IOException ioException) {
                    log.warn("⚠️ 임시 녹화 파일 삭제 실패: {}", tempFile.getAbsolutePath(), ioException);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload-recording")
    public ResponseEntity<String> uploadRecording(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 비어있습니다.");
            }

            log.info("🎥 녹화 파일 업로드 요청: {} (크기: {} bytes)", file.getOriginalFilename(), file.getSize());

            // 업로드된 파일의 Content-Type에 따라 확장자 결정
            String contentType = file.getContentType();
            String fileExtension = ".webm"; // 기본값
            if (contentType != null) {
                if (contentType.contains("mp4")) {
                    fileExtension = ".mp4";
                } else if (contentType.contains("webm")) {
                    fileExtension = ".webm";
                }
            }
            log.info("🎥 업로드된 파일 형식: {}, 확장자: {}", contentType, fileExtension);

            // 임시 파일 생성
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = "recording_" + sessionId + "_" + System.currentTimeMillis() + fileExtension;
            Path tempFilePath = Paths.get(tempDir, fileName);

            // MultipartFile을 임시 파일로 저장
            Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("📁 임시 파일 저장: {}", tempFilePath);

            // 사용자 ID 추출 (로그인한 경우)
            Long userId = null;
            if (userDetails != null) {
                try {
                    userId = Long.parseLong(userDetails.getUsername());
                    log.info("👤 로그인한 사용자 ID: {}", userId);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ 사용자 ID 파싱 실패: {}", userDetails.getUsername());
                }
            }

            // S3에 업로드
            String recordingId = sessionId + "_" + System.currentTimeMillis();
            String s3Key = s3UploadService.uploadRecordingFile(recordingId, tempFilePath.toString(), userId);
            String publicUrl = s3UploadService.getPublicUrl(s3Key);

            // 데이터베이스에 저장
            if (userId != null) {
                com.codelab.micproject.account.user.domain.User user = new com.codelab.micproject.account.user.domain.User();
                user.setId(userId);
                PracticeVideo practiceVideo = new PracticeVideo(publicUrl, user);
                practiceVideoRepository.save(practiceVideo);
                log.info("💾 데이터베이스에 녹화 정보 저장 완료");
            } else {
                // 익명 사용자의 경우에도 저장 (user 없이)
                PracticeVideo practiceVideo = new PracticeVideo(publicUrl);
                practiceVideoRepository.save(practiceVideo);
                log.info("💾 데이터베이스에 녹화 정보 저장 완료 (익명)");
            }

            // 임시 파일 삭제
            try {
                Files.deleteIfExists(tempFilePath);
                log.debug("🗑️ 임시 파일 삭제: {}", tempFilePath);
            } catch (IOException e) {
                log.warn("⚠️ 임시 파일 삭제 실패: {}", tempFilePath, e);
            }

            log.info("🎉 녹화 파일 업로드 완료: {}", publicUrl);
            return ResponseEntity.ok(publicUrl);

        } catch (Exception e) {
            log.error("❌ 녹화 파일 업로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/recordings")
    public ResponseEntity<?> getRecordings() {
        try {
            log.info("📹 녹화 파일 목록 요청");

            List<Map<String, Object>> recordings = new ArrayList<>();

            // S3에서 녹화 파일 목록 가져오기
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request listRequest =
                software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                    .bucket("micprojectbucket")
                    .prefix("recordings/")
                    .build();

            software.amazon.awssdk.services.s3.model.ListObjectsV2Response listResponse =
                s3UploadService.getS3Client().listObjectsV2(listRequest);

            for (software.amazon.awssdk.services.s3.model.S3Object object : listResponse.contents()) {
                // MP4와 WebM 파일 모두 지원
                if (object.key().endsWith(".webm") || object.key().endsWith(".mp4")) {
                    Map<String, Object> recording = new HashMap<>();
                    recording.put("key", object.key());
                    recording.put("url", s3UploadService.getPublicUrl(object.key()));
                    recording.put("size", object.size());
                    recording.put("lastModified", object.lastModified().toString());
                    recordings.add(recording);
                }
            }

            log.info("✅ 녹화 파일 {} 개 조회 완료", recordings.size());
            return ResponseEntity.ok(recordings);

        } catch (Exception e) {
            log.error("❌ 녹화 파일 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get recordings: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/recordings/video", method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Resource> streamVideo(@RequestParam(value = "url", required = false) String videoUrl, HttpServletRequest request) {
        // OPTIONS 요청 처리 (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            return ResponseEntity.ok().headers(headers).build();
        }

        try {
            log.info("🎬 비디오 스트리밍 요청: {}", videoUrl);

            // S3 객체 키 추출
            String key = videoUrl.replace("https://micprojectbucket.s3.amazonaws.com/", "");
            log.info("📁 S3 키: {}", key);

            // 먼저 파일 크기 및 메타데이터 조회 (HEAD 요청)
            software.amazon.awssdk.services.s3.model.HeadObjectRequest headRequest =
                software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                    .bucket("micprojectbucket")
                    .key(key)
                    .build();

            software.amazon.awssdk.services.s3.model.HeadObjectResponse headResponse =
                s3UploadService.getS3Client().headObject(headRequest);

            long fileSize = headResponse.contentLength();
            String actualContentType = headResponse.contentType();

            // Range 헤더 파싱
            String rangeHeader = request.getHeader("Range");
            long rangeStart = 0;
            long rangeEnd = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                try {
                    if (!ranges[0].isEmpty()) {
                        rangeStart = Long.parseLong(ranges[0]);
                    }
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        rangeEnd = Long.parseLong(ranges[1]);
                    }
                    log.info("📊 Range 요청: {}-{}/{}", rangeStart, rangeEnd, fileSize);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ 잘못된 Range 헤더: {}", rangeHeader);
                    rangeStart = 0;
                    rangeEnd = fileSize - 1;
                }
            }

            // Range가 파일 크기를 초과하지 않도록 조정
            rangeEnd = Math.min(rangeEnd, fileSize - 1);
            long contentLength = rangeEnd - rangeStart + 1;

            // S3 Range 요청으로 부분 데이터 가져오기
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest =
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket("micprojectbucket")
                    .key(key)
                    .range("bytes=" + rangeStart + "-" + rangeEnd)
                    .build();

            software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Object =
                s3UploadService.getS3Client().getObject(getObjectRequest);

            // Resource 생성
            InputStreamResource resource = new InputStreamResource(s3Object);

            // Content-Type 결정
            MediaType contentType;
            if (actualContentType != null && !actualContentType.isEmpty()) {
                contentType = MediaType.parseMediaType(actualContentType);
                log.info("📍 S3에서 가져온 Content-Type 사용: {}", actualContentType);
            } else if (key.endsWith(".mp4")) {
                contentType = MediaType.parseMediaType("video/mp4");
                log.info("📍 파일 확장자 기반 Content-Type: video/mp4");
            } else if (key.endsWith(".webm")) {
                contentType = MediaType.parseMediaType("video/webm");
                log.info("📍 파일 확장자 기반 Content-Type: video/webm");
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
                log.info("📍 기본 Content-Type 사용: application/octet-stream");
            }

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentLength(contentLength);
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");

            // Range 요청인 경우 206 Partial Content 응답
            if (rangeHeader != null) {
                headers.add("Content-Range", String.format("bytes %d-%d/%d", rangeStart, rangeEnd, fileSize));
                log.info("🎯 Range 응답: 206 Partial Content, bytes {}-{}/{}", rangeStart, rangeEnd, fileSize);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);
            } else {
                log.info("🎬 전체 파일 응답: 200 OK, {} bytes", contentLength);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            }

        } catch (Exception e) {
            log.error("❌ 비디오 스트리밍 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/recordings/clear-all")
    public ResponseEntity<String> deleteAllRecordings() {
        try {
            log.info("🗑️ 모든 녹화 파일 삭제 요청");

            // S3에서 recordings/ 폴더의 모든 객체 조회
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request listRequest =
                software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                    .bucket("micprojectbucket")
                    .prefix("recordings/")
                    .build();

            software.amazon.awssdk.services.s3.model.ListObjectsV2Response listResponse =
                s3UploadService.getS3Client().listObjectsV2(listRequest);

            // 삭제할 객체가 없는 경우
            if (listResponse.contents().isEmpty()) {
                log.info("📭 삭제할 녹화 파일이 없습니다");
                return ResponseEntity.ok("삭제할 녹화 파일이 없습니다.");
            }

            // 모든 객체 삭제
            int deletedCount = 0;
            for (software.amazon.awssdk.services.s3.model.S3Object object : listResponse.contents()) {
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest =
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket("micprojectbucket")
                        .key(object.key())
                        .build();

                s3UploadService.getS3Client().deleteObject(deleteRequest);
                log.info("🗑️ 삭제됨: {}", object.key());
                deletedCount++;
            }

            String message = String.format("✅ %d개의 녹화 파일이 성공적으로 삭제되었습니다.", deletedCount);
            log.info(message);
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("❌ 녹화 파일 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("삭제 실패: " + e.getMessage());
        }
    }
}

