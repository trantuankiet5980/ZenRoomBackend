package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;

public record OverviewStatsDTO(
        long totalUsers,
        long activeUsers,
        long landlords,
        long tenants,
        long totalProperties,
        long approvedProperties,
        long pendingProperties,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        BigDecimal totalRevenue
) {}