package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.services.AdminStatsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService service;

    // 1) Tổng quan
    @GetMapping("/stats/overview")
    public ResponseEntity<OverviewStatsDTO> overview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        validateMonth(month);
        return ResponseEntity.ok(service.getOverview(year, month));
    }

    @GetMapping("/stats/revenue/summary")
    public ResponseEntity<RevenueStatsDTO> revenueSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day
    ) {
        validateMonth(month);
        validateDay(day);
        try {
            return ResponseEntity.ok(service.getRevenueStats(year, month, day));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // 2) Doanh thu theo ngày (last N days)
    @GetMapping("/stats/revenue")
    public ResponseEntity<List<DailyRevenueDTO>> revenue(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(service.getRevenue(days));
    }

    @GetMapping("/stats/posts/summary")
    public ResponseEntity<PostStatsDTO> postSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day
    ) {
        validateMonth(month);
        validateDay(day);
        try {
            return ResponseEntity.ok(service.getPostStats(year, month, day));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    // 3) Booking gần đây
    @GetMapping("/bookings/recent")
    public ResponseEntity<List<RecentBookingDTO>> recentBookings(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.getRecentBookings(limit));
    }

    private static void validateMonth(Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be between 1 and 12");
        }
    }

    private static void validateDay(Integer day) {
        if (day != null && (day < 1 || day > 31)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "day must be between 1 and 31");
        }
    }
}
