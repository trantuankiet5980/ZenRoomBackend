package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Invoice}
 */
@Value
public class InvoiceDto implements Serializable {
    String invoiceId;
    String invoiceNo;
    InvoiceStatus status;
    BookingDto booking;
    String tenantName;
    String tenantEmail;
    String tenantPhone;
    String landlordName;
    String landlordEmail;
    String landlordPhone;
    String propertyTitle;
    String propertyAddressText;
    BigDecimal subtotal;
    BigDecimal discount;
    BigDecimal tax;
    BigDecimal total;
    BigDecimal dueAmount;
    String paymentMethod;
    String paymentRef;
    LocalDateTime paidAt;
    LocalDateTime issuedAt;
    LocalDateTime dueAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String itemsJson;
}