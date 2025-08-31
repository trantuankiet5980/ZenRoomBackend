package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.MediaStatus;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="PropertyMedia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyMedia {
    @Id
    @Column(name="media_id", columnDefinition="CHAR(36)")
    private String mediaId;
    @PrePersist
    void pre(){ if(mediaId==null) mediaId= UUID.randomUUID().toString(); }

    @ManyToOne @JoinColumn(name="property_id") private Property property;

    @Enumerated(EnumType.STRING) private MediaType mediaType;
    private String url;
    private String mimeType;
    private Long bytes;
    private Integer width;
    private Integer height;
    private BigDecimal aspectRatio;
    private BigDecimal durationSec;
    private String posterUrl;
    private Integer sortOrder;
    private Boolean isCover;
    @Enumerated(EnumType.STRING) private MediaStatus status;
}
