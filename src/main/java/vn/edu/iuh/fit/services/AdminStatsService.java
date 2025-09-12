package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.DailyRevenueDTO;
import vn.edu.iuh.fit.dtos.OverviewStatsDTO;
import vn.edu.iuh.fit.dtos.RecentBookingDTO;

import java.util.List;

public interface AdminStatsService {
    OverviewStatsDTO getOverview();
    List<DailyRevenueDTO> getRevenue(int days);
    List<RecentBookingDTO> getRecentBookings(int limit);
}
