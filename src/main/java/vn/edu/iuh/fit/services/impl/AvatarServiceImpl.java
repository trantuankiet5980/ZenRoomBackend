package vn.edu.iuh.fit.services.impl;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.AvatarService;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class AvatarServiceImpl implements AvatarService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final UserRepository userRepo;
    private final AuthService authService;

    // Các biến cấu hình đọc từ .env qua java-dotenv
    private final String bucket;
    private final String region;
    private final boolean bucketPublic;
    private final String baseUrl;

    public AvatarServiceImpl(
            S3Client s3,
            S3Presigner presigner,
            UserRepository userRepo,
            AuthService authService
    ) {
        this.s3 = s3;
        this.presigner = presigner;
        this.userRepo = userRepo;
        this.authService = authService;

        Dotenv env = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()    // không ném lỗi nếu không có file .env (có thể đang dùng env hệ thống)
                .load();

        this.bucket = env.get("AWS_S3_BUCKET");
        this.region = env.get("AWS_REGION");
        this.baseUrl = env.get("AWS_S3_BASE_URL");
        this.bucketPublic = Boolean.parseBoolean(env.get("AWS_S3_BUCKET_PUBLIC", "false"));

        log.info("AvatarService: bucket={}, region={}, public={}, baseUrl={}",
                bucket, region, bucketPublic, (baseUrl != null ? baseUrl : "<null>"));
    }

    @Transactional
    @Override
    public String uploadMyAvatar(MultipartFile file) {
        User me = authService.getCurrentUser();
        return uploadAndUpdate(me, file);
    }

    @Transactional
    @Override
    public String uploadAvatarForUser(String userId, MultipartFile file) {
        User u = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return uploadAndUpdate(u, file);
    }

    @Transactional(readOnly = true)
    @Override
    public String presignMyAvatar(long minutesTtl) {
        User me = authService.getCurrentUser();
        if (me.getAvatarUrl() == null || me.getAvatarUrl().isBlank()) return null;

        if (bucketPublic) {
            // avatarUrl đang là public URL
            return me.getAvatarUrl();
        } else {
            String key = me.getAvatarUrl(); // DB lưu key khi bucket private
            return presign(key, Duration.ofMinutes(Math.max(1, minutesTtl)));
        }
    }

    /* -------------------- helpers -------------------- */

    private String uploadAndUpdate(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image");
        }
        try {
            // 1) Xóa avatar cũ (nếu có)
            deleteOldIfAny(user);

            // 2) Tạo key mới và put lên S3
            String key = buildKey(user.getUserId(), file.getOriginalFilename());
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket).key(key)
                    .contentType(ct)
                    .cacheControl("public, max-age=31536000")
                    .build();
            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

            // 3) Lưu DB: public => lưu URL; private => lưu key
            String stored = bucketPublic ? baseUrlWithSlash() + key : key;
            user.setAvatarUrl(stored);
            userRepo.save(user);

            // 4) Trả về URL cho client
            if (bucketPublic) {
                return stored;
            } else {
                // private: trả link presigned 15 phút
                return presign(key, Duration.ofMinutes(15));
            }
        } catch (Exception e) {
            throw new RuntimeException("Upload avatar failed: " + e.getMessage(), e);
        }
    }

    private void deleteOldIfAny(User user) {
        String current = user.getAvatarUrl();
        if (current == null || current.isBlank()) return;
        try {
            String key = bucketPublic ? extractKeyFromPublicUrl(current) : current;
            if (key != null && !key.isBlank()) {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            }
        } catch (Exception ignore) {
            // Không fail request chỉ vì không xoá được ảnh cũ
        }
    }

    private String buildKey(String userId, String originalName) {
        String ext = getExt(originalName);
        String uuid = UUID.randomUUID().toString();
        return String.format("avatars/%s/%s%s", userId, uuid, ext);
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : "";
    }

    private String baseUrlWithSlash() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        }
        return baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
    }

    private String extractKeyFromPublicUrl(String url) {
        String prefix = baseUrlWithSlash();
        return url.startsWith(prefix) ? url.substring(prefix.length()) : url;
    }

    private String presign(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(req).url().toString();
    }
}
