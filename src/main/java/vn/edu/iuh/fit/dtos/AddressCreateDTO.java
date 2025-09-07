package vn.edu.iuh.fit.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressCreateDTO {
    private String addressId;
    private String countryCode;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String addressFull;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
