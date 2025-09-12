package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.DailyRevenueDTO;
import vn.edu.iuh.fit.dtos.OverviewStatsDTO;
import vn.edu.iuh.fit.dtos.RecentBookingDTO;
import vn.edu.iuh.fit.repositories.AdminStatsRepository;
import vn.edu.iuh.fit.services.AdminStatsService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final AdminStatsRepository repo;

    @Override
    public OverviewStatsDTO getOverview() {
        return repo.getOverview();
    }

    @Override
    public List<DailyRevenueDTO> getRevenue(int days) {
        return repo.getDailyRevenueLastNDays(days <= 0 ? 30 : days);
    }

    @Override
    public List<RecentBookingDTO> getRecentBookings(int limit) {
        return repo.getRecentBookings(limit <= 0 ? 10 : limit);
    }
}
