package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.util.UUID;

@Entity
@Table(name = "property_media",
        indexes = {
                @Index(name="idx_media_property", columnList = "property_id"),
                @Index(name="idx_media_sort", columnList = "sortOrder")
        })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyMedia {

    @Id
    @Column(name="media_id", columnDefinition="CHAR(36)")
    private String mediaId;

    @PrePersist
    void pre(){ if (mediaId == null) mediaId = UUID.randomUUID().toString(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType mediaType;  // IMAGE | VIDEO

    @Column(nullable = false, length = 1000)
    private String url;           // S3 URL

    private String posterUrl;     // thumbnail cho video (optional)
    private Integer sortOrder;    // sắp xếp
    private Boolean isCover;      // ảnh đại diện
}
