package vn.edu.iuh.fit.dtos;

import lombok.Data;
import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.io.Serializable;
import java.math.BigDecimal;

@Value
public class PropertyServiceItemDto implements Serializable {
    String id;
    String serviceName;
    BigDecimal fee;
    ChargeBasis chargeBasis;
    Boolean isIncluded;
    String note;
}
