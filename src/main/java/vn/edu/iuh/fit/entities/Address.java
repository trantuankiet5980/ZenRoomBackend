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

    @Column(name = "country_code", length = 2)
    private String countryCode; // VN, US, ...

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "house_number", length = 30)
    private String houseNumber;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_full", length = 255)
    private String addressFull;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
}
