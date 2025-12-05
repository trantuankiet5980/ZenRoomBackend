package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;

public record LandlordMonthlyPayoutDTO(
        String landlordId,
        String landlordName,
        BigDecimal paidAmount,
        BigDecimal platformFee,
        BigDecimal landlordReceivable
) {
}
