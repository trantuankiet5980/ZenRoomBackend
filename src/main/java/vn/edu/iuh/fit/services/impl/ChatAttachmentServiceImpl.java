package vn.edu.iuh.fit.services.impl;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import vn.edu.iuh.fit.entities.MessageAttachment;
import vn.edu.iuh.fit.entities.enums.MediaType;
import vn.edu.iuh.fit.services.ChatAttachmentService;
import vn.edu.iuh.fit.services.ChatService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ChatAttachmentServiceImpl implements ChatAttachmentService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String region;
    private final boolean bucketPublic;
    private final String baseUrl;

    public ChatAttachmentServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;

        Dotenv env = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        this.bucket = require(env, "AWS_S3_BUCKET");
        this.region = env.get("AWS_REGION", "ap-southeast-1");
        String publicStr = env.get("AWS_S3_BUCKET_PUBLIC");
        if (publicStr == null) {
            publicStr = env.get("AWS_S3_PUBLIC", "false");
        }
        this.bucketPublic = parseBool(publicStr);
        this.baseUrl = env.get("AWS_S3_BASE_URL", "");

        log.info("[ChatAttachmentService] bucket={}, region={}, public={}, baseUrl={}"
                , bucket, region, bucketPublic, baseUrl);
    }

    @Override
    public List<ChatService.AttachmentPayload> uploadImages(String senderId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No images uploaded");
        }
        List<ChatService.AttachmentPayload> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = safeContentType(file.getContentType());
            if (!contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            String key = buildKey(senderId, file.getOriginalFilename());
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000")
                    .build();
            try {
                s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
            }

            String storedUrl = bucketPublic ? baseUrlWithSlash() + key : null;
            result.add(new ChatService.AttachmentPayload(
                    key,
                    storedUrl,
                    MediaType.IMAGE,
                    contentType,
                    file.getSize()
            ));
        }
        return result;
    }

    @Override
    public String resolveUrl(MessageAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
            return attachment.getUrl();
        }
        String key = attachment.getStorageKey();
        if (key == null || key.isBlank()) {
            return null;
        }
        if (bucketPublic) {
            return baseUrlWithSlash() + key;
        }
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(get)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String buildKey(String senderId, String originalName) {
        String ext = getExt(originalName);
        return String.format("chats/%s/%s%s", senderId != null ? senderId : "anonymous", UUID.randomUUID(), ext);
    }

    private String getExt(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx) : "";
    }

    private String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private static boolean parseBool(String v) {
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }

    private String baseUrlWithSlash() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        }
        return baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
    }

    private static String require(Dotenv env, String key) {
        String v = env.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env: " + key);
        }
        return v;
    }
}
