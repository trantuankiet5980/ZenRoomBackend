package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.util.List;

public record AdminLandlordMonthlyPayoutDTO(
        int year,
        int month,
        BigDecimal totalPaidAmount,
        BigDecimal totalPlatformFee,
        BigDecimal totalLandlordReceivable,
        List<LandlordMonthlyPayoutDTO> landlords
) {
}
