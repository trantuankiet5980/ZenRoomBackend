package vn.edu.iuh.fit.dtos;

import java.util.List;

public record PostStatsDTO(
        StatPeriod period,
        int year,
        Integer month,
        Integer day,
        long totalPosts,
        List<DailyPostCountDTO> dailyBreakdown,
        List<MonthlyPostCountDTO> monthlyBreakdown
) {
}
