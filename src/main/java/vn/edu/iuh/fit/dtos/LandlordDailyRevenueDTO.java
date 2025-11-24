package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LandlordDailyRevenueDTO(
        LocalDate date,
        BigDecimal landlordReceivable,
        BigDecimal platformFee
) {
}
