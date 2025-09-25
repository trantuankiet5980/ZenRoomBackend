package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "addresses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_code")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code")
    private Ward ward;

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

    public void generateAddressFull() {
        StringBuilder full = new StringBuilder();
        if (houseNumber != null && !houseNumber.isEmpty()) full.append(houseNumber).append(", ");
        if (street != null && !street.isEmpty()) full.append(street).append(", ");
        if (ward != null && ward.getName() != null) full.append(ward.getName()).append(", ");
        if (district != null && district.getName() != null) full.append(district.getName()).append(", ");
        if (province != null && province.getName() != null) full.append(province.getName());

        // loại bỏ ", " thừa cuối cùng
        this.addressFull = full.toString().replaceAll(", $", "");
    }
}
