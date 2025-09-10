package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @Column(name = "address_id", columnDefinition = "CHAR(36)")
    private String addressId;

    @PrePersist
    public void prePersist() {
        if (this.addressId == null || this.addressId.isEmpty()) {
            this.addressId = java.util.UUID.randomUUID().toString();
        }
    }

    // Cấp tỉnh/thành
    @Column(name = "province", length = 100)
    private String province;

    // Cấp quận/huyện/thị xã
    @Column(name = "district", length = 100)
    private String district;

    // Cấp phường/xã/thị trấn
    @Column(name = "ward", length = 100)
    private String ward;

    // Đường & số nhà
    @Column(name = "street", length = 150)
    private String street;
    @Column(name = "house_number", length = 30)
    private String houseNumber;

    @Column(name = "address_full", length = 255)
    private String addressFull;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
}
