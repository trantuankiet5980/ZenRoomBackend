package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Service}
 */
@Value
public class ServiceDto implements Serializable {
    String serviceId;
    String serviceName;
    BigDecimal defaultFee;
    ChargeBasis chargeBasis;
    String notes;
    List<PropertyServiceDto> properties;
}