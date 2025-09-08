package vn.edu.iuh.fit.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Address}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto implements Serializable {
    String addressId;
    String countryCode;
    String province;
    String district;
    String ward;
    String street;
    String houseNumber;
    String postalCode;
    String addressFull;
    BigDecimal latitude;
    BigDecimal longitude;
}