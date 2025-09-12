package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.DailyRevenueDTO;
import vn.edu.iuh.fit.dtos.OverviewStatsDTO;
import vn.edu.iuh.fit.dtos.RecentBookingDTO;
import vn.edu.iuh.fit.services.AdminStatsService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService service;

    // 1) Tổng quan
    @GetMapping("/stats/overview")
    public ResponseEntity<OverviewStatsDTO> overview() {
        return ResponseEntity.ok(service.getOverview());
    }

    // 2) Doanh thu theo ngày (last N days)
    @GetMapping("/stats/revenue")
    public ResponseEntity<List<DailyRevenueDTO>> revenue(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(service.getRevenue(days));
    }

    // 3) Booking gần đây
    @GetMapping("/bookings/recent")
    public ResponseEntity<List<RecentBookingDTO>> recentBookings(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.getRecentBookings(limit));
    }
}
