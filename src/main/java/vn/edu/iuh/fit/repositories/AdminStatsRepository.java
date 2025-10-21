package vn.edu.iuh.fit.repositories;

import vn.edu.iuh.fit.dtos.*;

import java.math.BigDecimal;
import java.util.List;

public interface AdminStatsRepository {
    OverviewStatsDTO getOverview(Integer year, Integer month);
    BigDecimal getRevenueLastNDays(int days);
    List<DailyRevenueDTO> getDailyRevenueLastNDays(int days);
    List<RecentBookingDTO> getRecentBookings(int limit);
    BigDecimal getRevenueForDay(java.time.LocalDate date);
    BigDecimal getRevenueForMonth(int year, int month);
    BigDecimal getRevenueForYear(int year);
    List<DailyRevenueDTO> getDailyRevenueForMonth(int year, int month);
    List<MonthlyRevenueDTO> getMonthlyRevenueForYear(int year);
    long getApprovedPostCountForDay(java.time.LocalDate date);
    long getApprovedPostCountForMonth(int year, int month);
    long getApprovedPostCountForYear(int year);
    List<DailyPostCountDTO> getApprovedPostDailyCountsForMonth(int year, int month);
    List<MonthlyPostCountDTO> getApprovedPostMonthlyCountsForYear(int year);
}
