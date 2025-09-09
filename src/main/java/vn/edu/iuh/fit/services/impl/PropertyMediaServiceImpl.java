package vn.edu.iuh.fit.services.impl;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
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
import vn.edu.iuh.fit.dtos.PropertyMediaDto;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.PropertyMedia;
import vn.edu.iuh.fit.entities.enums.MediaType;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.mappers.PropertyMediaMapper;
import vn.edu.iuh.fit.repositories.PropertyMediaRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.services.PropertyMediaService;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PropertyMediaServiceImpl implements PropertyMediaService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final PropertyRepository propertyRepo;
    private final PropertyMediaRepository mediaRepo;
    private final EntityManager em;

    // Các biến cấu hình đọc từ .env qua java-dotenv
    private final String bucket;
    private final String region;
    private final boolean bucketPublic;
    private final String baseUrl;

    public PropertyMediaServiceImpl(
            S3Client s3,
            S3Presigner presigner,
            PropertyRepository propertyRepo,
            PropertyMediaRepository mediaRepo,
            EntityManager em
    ) {
        this.s3 = s3;
        this.presigner = presigner;
        this.propertyRepo = propertyRepo;
        this.mediaRepo = mediaRepo;
        this.em = em;

        // Load .env (không lỗi nếu thiếu file, nhưng sẽ kiểm tra biến bắt buộc bên dưới)
        Dotenv env = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()    // không ném lỗi nếu không có file .env (có thể đang dùng env hệ thống)
                .load();

        this.bucket = require(env, "AWS_S3_BUCKET");
        this.region = env.get("AWS_REGION", "ap-southeast-2");
        this.bucketPublic = parseBool(env.get("AWS_S3_PUBLIC", "false"));
        this.baseUrl = env.get("AWS_S3_BASE_URL", "");

        log.info("[PropertyMediaService] Config loaded: bucket={}, region={}, public={}, baseUrl={}",
                bucket, region, bucketPublic, baseUrl);
    }

    @Transactional
    @Override
    public PropertyMedia upload(String propertyId, MultipartFile file, MediaType mediaType, Integer sortOrder, Boolean isCover) throws IOException {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        String key = buildKey(property, mediaType, file.getOriginalFilename());

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .contentType(safeContentType(file.getContentType()))
                .cacheControl("public, max-age=31536000")
                .build();

        s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

        if (Boolean.TRUE.equals(isCover)) {
            mediaRepo.clearCover(propertyId);
        }

        String storedUrlOrKey = bucketPublic ? baseUrlWithSlash() + key : key;

        PropertyMedia media = PropertyMedia.builder()
                .property(em.getReference(Property.class, propertyId))
                .mediaType(mediaType)
                .url(storedUrlOrKey)
                .posterUrl(null)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isCover(Boolean.TRUE.equals(isCover))
                .build();

        return mediaRepo.save(media);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PropertyMediaDto> list(String propertyId, boolean presign, PropertyMediaMapper mapper) {
        return mediaRepo.findByProperty_PropertyIdOrderBySortOrderAsc(propertyId).stream()
                .map(m -> {
                    PropertyMediaDto dto = mapper.toDto(m);

                    // bucket private + yêu cầu presign -> trả link tạm thời
                    if (!bucketPublic && presign) {
                        String key = m.getUrl(); // DB đang lưu key khi private
                        dto = new PropertyMediaDto(
                                dto.getMediaId(),
                                dto.getPropertyId(),
                                dto.getMediaType(),
                                generatePresignedUrl(key, Duration.ofMinutes(15)),
                                dto.getPosterUrl(),
                                dto.getSortOrder(),
                                dto.getIsCover()
                        );
                    }
                    return dto;
                })
                .toList();
    }

    @Transactional
    @Override
    public void setCover(String propertyId, String mediaId) {
        PropertyMedia target = mediaRepo.findByMediaIdAndProperty_PropertyId(mediaId, propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found in this property"));

        mediaRepo.clearCover(propertyId);
        target.setIsCover(true);
        mediaRepo.save(target);
    }

    @Transactional
    @Override
    public void delete(String mediaId) {
        PropertyMedia media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        String key = bucketPublic ? extractKeyFromPublicUrl(media.getUrl()) : media.getUrl();
        if (key != null && !key.isBlank()) {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
        mediaRepo.delete(media);
    }

    /* -------------- helpers -------------- */

    private String buildKey(Property property, MediaType type, String originalName) {
        String ext = getExt(originalName);
        String uuid = UUID.randomUUID().toString();
        String folder = (type == MediaType.IMAGE) ? "gallery" : "videos";

        if (property.getPropertyType() == PropertyType.ROOM) {
            String buildingId = property.getParent() != null ? property.getParent().getPropertyId() : "unknown";
            String roomId = property.getPropertyId();
            return String.format("properties/%s/rooms/%s/%s/%s%s", buildingId, roomId, folder, uuid, ext);
        } else {
            String buildingId = property.getPropertyId();
            return String.format("properties/%s/%s/%s%s", buildingId, folder, uuid, ext);
        }
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : "";
    }

    private String baseUrlWithSlash() {
        // nếu baseUrl trống và bucketPublic=true -> fallback URL chuẩn S3
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        }
        return baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
    }

    private String extractKeyFromPublicUrl(String url) {
        String prefix = baseUrlWithSlash();
        return url.startsWith(prefix) ? url.substring(prefix.length()) : url;
    }

    private String generatePresignedUrl(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    private String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private static String require(Dotenv env, String key) {
        String v = env.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env: " + key);
        }
        return v;
    }

    private static boolean parseBool(String v) {
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }
}
