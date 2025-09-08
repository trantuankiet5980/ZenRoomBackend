package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.PropertyService}
 */
@Value
public class PropertyServiceDto implements Serializable {
    String id;
    PropertyDto property;
    BigDecimal fee;
    ChargeBasis chargeBasis;
    Boolean isIncluded;
    String note;
}