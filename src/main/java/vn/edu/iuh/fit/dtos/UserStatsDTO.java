package vn.edu.iuh.fit.dtos;

import java.util.List;

public record UserStatsDTO(
        StatPeriod period,
        int year,
        Integer month,
        Integer day,
        long totalUsers,
        List<DailyUserCountDTO> dailyBreakdown,
        List<MonthlyUserCountDTO> monthlyBreakdown
) {
}
