package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Contract}
 */
@Value
public class ContractDto implements Serializable {
    String contractId;
    String tenantName;
    String tenantPhone;
    String tenantCccdFront;
    String tenantCccdBack;
    String title;
    String roomNumber;
    String buildingName;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal rentPrice;
    BigDecimal deposit;
    LocalDate billingStartDate;
    Integer paymentDueDay;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}