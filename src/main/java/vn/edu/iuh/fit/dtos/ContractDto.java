package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ContractStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Contract}
 */
@Value
public class ContractDto implements Serializable {
    String contractId;
    BookingDto booking;
    String tenantName;
    String tenantPhone;
    String tenantCccdFront;
    String tenantCccdBack;
    String title;
    String roomNumber;
    String buildingName;
    LocalDate startDate;
    LocalDate endDate;
    ContractStatus contractStatus;
    List<ContractServiceDto> services;
    BigDecimal rentPrice;
    BigDecimal deposit;
    LocalDate billingStartDate;
    Integer paymentDueDay;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}