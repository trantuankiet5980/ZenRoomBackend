package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.repositories.AdminStatsRepository;
import vn.edu.iuh.fit.services.AdminStatsService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final AdminStatsRepository repo;

    @Override
    public OverviewStatsDTO getOverview(Integer year, Integer month) {
        return repo.getOverview(year, month);
    }

    @Override
    public List<DailyRevenueDTO> getRevenue(int days) {
        return repo.getDailyRevenueLastNDays(days <= 0 ? 30 : days);
    }

    @Override
    public List<RecentBookingDTO> getRecentBookings(int limit) {
        return repo.getRecentBookings(limit <= 0 ? 10 : limit);
    }

    @Override
    public RevenueStatsDTO getRevenueStats(Integer year, Integer month, Integer day) {
        var today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        Integer resolvedMonth = month;
        Integer resolvedDay = day;

        if (resolvedDay != null) {
            if (resolvedMonth == null) {
                resolvedMonth = today.getMonthValue();
            }
            if (year == null) {
                resolvedYear = today.getYear();
            }

            LocalDate targetDate = validateDate(resolvedYear, resolvedMonth, resolvedDay);
            var total = repo.getRevenueForDay(targetDate);
            return new RevenueStatsDTO(
                    StatPeriod.DAY,
                    resolvedYear,
                    resolvedMonth,
                    resolvedDay,
                    total,
                    List.of(new DailyRevenueDTO(targetDate, total)),
                    List.of()
            );
        }

        if (resolvedMonth != null) {
            if (year == null) {
                resolvedYear = today.getYear();
            }
            validateMonth(resolvedMonth);
            var total = repo.getRevenueForMonth(resolvedYear, resolvedMonth);
            var daily = repo.getDailyRevenueForMonth(resolvedYear, resolvedMonth);
            return new RevenueStatsDTO(
                    StatPeriod.MONTH,
                    resolvedYear,
                    resolvedMonth,
                    null,
                    total,
                    daily,
                    List.of()
            );
        }

        var total = repo.getRevenueForYear(resolvedYear);
        var monthly = repo.getMonthlyRevenueForYear(resolvedYear);
        return new RevenueStatsDTO(
                StatPeriod.YEAR,
                resolvedYear,
                null,
                null,
                total,
                List.of(),
                monthly
        );
    }

    @Override
    public PostStatsDTO getPostStats(Integer year, Integer month, Integer day) {
        var today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        Integer resolvedMonth = month;
        Integer resolvedDay = day;

        if (resolvedDay != null) {
            if (resolvedMonth == null) {
                resolvedMonth = today.getMonthValue();
            }
            if (year == null) {
                resolvedYear = today.getYear();
            }

            LocalDate targetDate = validateDate(resolvedYear, resolvedMonth, resolvedDay);
            long totalPosts = repo.getApprovedPostCountForDay(targetDate);
            return new PostStatsDTO(
                    StatPeriod.DAY,
                    resolvedYear,
                    resolvedMonth,
                    resolvedDay,
                    totalPosts,
                    List.of(new DailyPostCountDTO(targetDate, totalPosts)),
                    List.of()
            );
        }

        if (resolvedMonth != null) {
            if (year == null) {
                resolvedYear = today.getYear();
            }
            validateMonth(resolvedMonth);
            long totalPosts = repo.getApprovedPostCountForMonth(resolvedYear, resolvedMonth);
            var daily = repo.getApprovedPostDailyCountsForMonth(resolvedYear, resolvedMonth);
            return new PostStatsDTO(
                    StatPeriod.MONTH,
                    resolvedYear,
                    resolvedMonth,
                    null,
                    totalPosts,
                    daily,
                    List.of()
            );
        }

        long totalPosts = repo.getApprovedPostCountForYear(resolvedYear);
        var monthly = repo.getApprovedPostMonthlyCountsForYear(resolvedYear);
        return new PostStatsDTO(
                StatPeriod.YEAR,
                resolvedYear,
                null,
                null,
                totalPosts,
                List.of(),
                monthly
        );
    }

    private static void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
    }

    private static LocalDate validateDate(int year, int month, int day) {
        validateMonth(month);
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("day must be between 1 and 31");
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        if (day > yearMonth.lengthOfMonth()) {
            throw new IllegalArgumentException("invalid day for month");
        }
        return LocalDate.of(year, month, day);
    }
}
