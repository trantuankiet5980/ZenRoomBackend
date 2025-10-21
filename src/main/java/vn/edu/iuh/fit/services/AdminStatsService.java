package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.*;

import java.util.List;

public interface AdminStatsService {
    OverviewStatsDTO getOverview(Integer year, Integer month);
    List<DailyRevenueDTO> getRevenue(int days);
    List<RecentBookingDTO> getRecentBookings(int limit);
    RevenueStatsDTO getRevenueStats(Integer year, Integer month, Integer day);
    PostStatsDTO getPostStats(Integer year, Integer month, Integer day);
    UserStatsDTO getUserStats(Integer year, Integer month, Integer day);
}
