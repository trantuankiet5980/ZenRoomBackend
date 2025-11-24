package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;

public record LandlordMonthlyRevenueDTO(
        int year,
        int month,
        BigDecimal landlordReceivable,
        BigDecimal platformFee
) {
}
