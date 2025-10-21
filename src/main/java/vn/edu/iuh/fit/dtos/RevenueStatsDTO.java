package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.util.List;

public record RevenueStatsDTO(
        StatPeriod period,
        int year,
        Integer month,
        Integer day,
        BigDecimal totalRevenue,
        List<DailyRevenueDTO> dailyBreakdown,
        List<MonthlyRevenueDTO> monthlyBreakdown
) {
}
