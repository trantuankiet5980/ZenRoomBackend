package vn.edu.iuh.fit.repositories;

import vn.edu.iuh.fit.dtos.DailyRevenueDTO;
import vn.edu.iuh.fit.dtos.OverviewStatsDTO;
import vn.edu.iuh.fit.dtos.RecentBookingDTO;

import java.math.BigDecimal;
import java.util.List;

public interface AdminStatsRepository {
    OverviewStatsDTO getOverview();
    BigDecimal getRevenueLastNDays(int days);
    List<DailyRevenueDTO> getDailyRevenueLastNDays(int days);
    List<RecentBookingDTO> getRecentBookings(int limit);
}
