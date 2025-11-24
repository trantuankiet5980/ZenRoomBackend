package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.util.List;

public record LandlordRevenueSummaryDTO(
        StatPeriod period,
        int year,
        Integer month,
        Integer day,
        BigDecimal totalLandlordReceivable,
        BigDecimal totalPlatformFee,
        List<LandlordDailyRevenueDTO> dailyBreakdown,
        List<LandlordMonthlyRevenueDTO> monthlyBreakdown,
        List<LandlordRevenueBookingDTO> bookings
) {
}
