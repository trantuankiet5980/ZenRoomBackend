package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;

public record TopBookedPropertyDTO(
        String propertyId,
        String propertyTitle,
        String buildingName,
        BigDecimal pricePerNight,
        long bookingCount,
        long totalBookedDays
) {
}
