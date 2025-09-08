package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.DiscountCodeStatus;
import vn.edu.iuh.fit.entities.enums.DiscountType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.DiscountCode}
 */
@Value
public class DiscountCodeDto implements Serializable {
    String codeId;
    String code;
    String description;
    DiscountType discountType;
    BigDecimal discountValue;
    LocalDate validFrom;
    LocalDate validTo;
    Integer usageLimit;
    Integer usedCount;
    DiscountCodeStatus status;
}