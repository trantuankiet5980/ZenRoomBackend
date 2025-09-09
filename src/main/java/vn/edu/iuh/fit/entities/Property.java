package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ApartmentCategory;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "properties",
        indexes = {
                @Index(name="idx_property_type", columnList = "propertyType"),
                @Index(name="idx_property_parent", columnList = "parent_id"),
                @Index(name="idx_property_landlord", columnList = "landlord_id"),
                @Index(name="idx_property_created", columnList = "createdAt")
        })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Property {

    @Id
    @Column(name = "property_id", columnDefinition = "CHAR(36)")
    private String propertyId;

    @PrePersist
    void prePersist() {
        if (propertyId == null) propertyId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (postStatus == null) postStatus = PostStatus.PENDING;
        validateByType();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        validateByType();
    }

    /* ===== Phân loại ===== */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyType propertyType;  // BUILDING | ROOM

    /* ===== Quan hệ cha–con (ROOM -> BUILDING) ===== */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Property parent;            // null nếu là BUILDING

    /* ===== Chủ nhà ===== */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    /* ===== Địa chỉ (cả BUILDING & ROOM) ===== */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /* ===== Thông tin dùng khi “đăng” (đưa thẳng vào Property) ===== */
    @Column(name = "title", length = 200, nullable = false)
    private String title;               // tiêu đề đăng

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;         // mô tả đăng

    /* ===== Thông tin chung (có thể dùng cho cả 2) ===== */
    private Double area;                // m2
    private BigDecimal price;           // giá thuê / tháng
    private BigDecimal deposit;         // tiền cọc

    /* ===== Riêng cho BUILDING (căn hộ/tòa) ===== */
    private String buildingName;        // tên tòa/căn hộ
    @Enumerated(EnumType.STRING)
    private ApartmentCategory apartmentCategory; // CHUNG_CU | DUPLEX | PENTHOUSE
    private Integer bedrooms;           // số phòng ngủ (nếu bạn áp cho building)
    private Integer bathrooms;          // số phòng vệ sinh (nếu bạn áp cho building)

    /* ===== Riêng cho ROOM (nếu cần thêm fields thì mở rộng) ===== */
    private String roomNumber;          // optional
    private Integer floorNo;            // optional

    /* ===== Nội thất / Tiện nghi / Dịch vụ ===== */
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyFurnishing> furnishings = new ArrayList<>();


    /* ===== Media: ảnh/video (S3 URLs) ===== */
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyMedia> media = new ArrayList<>();

    /* ===== Trạng thái duyệt bài ===== */
    @Enumerated(EnumType.STRING)
    @Column(name="post_status", length=20, nullable=false)
    private PostStatus postStatus;

    @Column(name="rejected_reason", columnDefinition="TEXT")
    private String rejectedReason;

    private LocalDateTime publishedAt;

    /* ===== Audit ===== */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ===== Ràng buộc theo loại ===== */
    private void validateByType() {
        if (propertyType == PropertyType.BUILDING) {
            // BUILDING: bắt buộc các thông tin riêng
            if (buildingName == null || buildingName.isBlank())
                throw new IllegalArgumentException("BUILDING: 'buildingName' is required");
            if (apartmentCategory == null)
                throw new IllegalArgumentException("BUILDING: 'apartmentCategory' is required");

             if (bedrooms == null) throw new IllegalArgumentException("BUILDING: 'bedrooms' is required");
             if (bathrooms == null) throw new IllegalArgumentException("BUILDING: 'bathrooms' is required");
        } else if (propertyType == PropertyType.ROOM) {

            if (area == null || area <= 0)
                throw new IllegalArgumentException("ROOM: 'area' is required and must be > 0");
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("ROOM: 'price' is required and must be > 0");
            if (deposit == null || deposit.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("ROOM: 'deposit' is required and must be >= 0");
        } else {
            throw new IllegalArgumentException("Unknown propertyType");
        }
    }
}
