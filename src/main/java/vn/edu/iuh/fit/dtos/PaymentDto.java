package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.PaymentMethod;
import vn.edu.iuh.fit.entities.enums.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Payment}
 */
@Value
public class PaymentDto implements Serializable {
    String paymentId;
    BookingDto booking;
    BigDecimal amount;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    String transactionId;
    BigDecimal refundedAmount;
    LocalDateTime createdAt;
}