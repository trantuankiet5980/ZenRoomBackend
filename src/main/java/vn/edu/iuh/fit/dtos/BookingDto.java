package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.BookingStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Booking}
 */
@Value
public class BookingDto implements Serializable {
    String bookingId;
    PropertyDto property;
    UserDto tenant;
    LocalDate startDate;
    LocalDate endDate;
    BookingStatus bookingStatus;
    BigDecimal totalPrice;
    String note;
    String paymentUrl;
    DiscountCodeDto discountCode;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String contractId;
}