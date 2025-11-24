package vn.edu.iuh.fit.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LandlordRevenueBookingDTO(
        String invoiceId,
        String invoiceNo,
        String bookingId,
        String propertyTitle,
        BigDecimal total,
        BigDecimal platformFee,
        BigDecimal landlordReceivable,
        LocalDate paidDate
) {
}
