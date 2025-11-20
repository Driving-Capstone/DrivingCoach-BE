package com.drivingcoach.backend.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Qualifier("s3BucketName")
    private final String bucket;

    /** (추가) 특정 폴더(prefix) 내의 파일 목록 조회 */
    public List<String> listKeys(String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();
            return s3Client.listObjectsV2(request).contents().stream()
                    .map(S3Object::key)
                    .sorted() // 타임스탬프 순 정렬 보장
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[S3] List keys failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** (추가) 파일 다운로드 */
    public void download(String key, File destination) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.getObject(request, ResponseTransformer.toFile(destination));
        } catch (Exception e) {
            log.error("[S3] Download failed: {}", key, e);
            throw new RuntimeException("S3 다운로드 실패");
        }
    }

    /* ========================= Upload ========================= */

    /** MultipartFile 업로드 (키 자동 생성) → S3 object key 반환 */
    public String upload(MultipartFile file, String keyPrefix) {
        String original = file.getOriginalFilename();
        String ext = extractExt(original);
        String key = buildKey(keyPrefix, ext);

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = guessContentType(original);
        }

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ) // 공개 필요 시 PUBLIC_READ 로 조정
                    .build();

            s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("[S3] Uploaded: s3://{}/{}", bucket, key);
            return key;
        } catch (IOException e) {
            log.error("[S3] Upload failed: {}", e.getMessage(), e);
            throw S3Exception.builder().message("S3 업로드에 실패했습니다.").build();
        }
    }

    /** File 업로드 (키 자동 생성) → S3 object key 반환 */
    public String upload(File file, String keyPrefix) {
        String ext = extractExt(file.getName());
        String key = buildKey(keyPrefix, ext);
        String contentType = guessContentType(file.getName());

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .build();

        s3Client.putObject(req, RequestBody.fromFile(file));
        log.info("[S3] Uploaded: s3://{}/{}", bucket, key);
        return key;
    }

    /** 바이트 배열 업로드 (키 직접 지정) */
    public void uploadBytes(byte[] bytes, String key, String contentType) {
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .build();
        s3Client.putObject(req, RequestBody.fromBytes(bytes));
        log.info("[S3] Uploaded bytes: s3://{}/{}", bucket, key);
    }

    /* ========================= Delete ========================= */

    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("[S3] Deleted: s3://{}/{}", bucket, key);
        } catch (S3Exception e) {
            log.warn("[S3] Delete failed for {}: {}", key, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw e;
        }
    }

    /* ========================= Get URL ========================= */

    /**
     * (사전 서명 아님) 퍼블릭 버킷/오브젝트에 한해 접근 가능한 정적 URL
     * - 오브젝트가 PRIVATE이면 접근 불가.
     */
    public URL buildPublicUrl(String key) {
        try {
            return new URL(String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucket, s3Client.serviceClientConfiguration().region().id(), key));
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 키/버킷으로 URL 생성 실패", e);
        }
    }

    /* ========================= Helpers ========================= */

    private String buildKey(String prefix, String ext) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "uploads" : prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        String uuid = UUID.randomUUID().toString();
        String filename = (ext == null || ext.isBlank()) ? uuid : (uuid + "." + ext);
        // 예: driving/2025-09-28/uuid.mp4 같이 날짜를 넣고 싶다면 여기서 조합
        return safePrefix + "/" + filename;
    }

    private String extractExt(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return (dot > -1 && dot < filename.length() - 1) ? filename.substring(dot + 1) : null;
    }

    private String guessContentType(String filename) {
        try {
            String probed = Files.probeContentType(new File(filename).toPath());
            if (probed != null) return probed;
        } catch (IOException ignored) {}
        // fallback by simple extension check
        String ext = extractExt(filename);
        if (ext == null) return "application/octet-stream";
        String e = ext.toLowerCase();
        return switch (e) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "zip" -> "application/zip";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}
