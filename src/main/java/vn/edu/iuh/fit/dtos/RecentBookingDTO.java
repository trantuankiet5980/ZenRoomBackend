package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentBookingDTO(
        String bookingId,
        String tenantName,
        String propertyTitle,
        BigDecimal totalPrice,
        String bookingStatus,
        LocalDateTime createdAt
) {}
