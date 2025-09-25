package vn.edu.iuh.fit.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto implements Serializable {
    private String addressId;

    // Lưu cả id & name để FE dễ sử dụng
    private String provinceId;
    private String provinceName;

    private String districtId;
    private String districtName;

    private String wardId;
    private String wardName;

    private String street;
    private String houseNumber;
    private String addressFull;

    private BigDecimal latitude;
    private BigDecimal longitude;
}