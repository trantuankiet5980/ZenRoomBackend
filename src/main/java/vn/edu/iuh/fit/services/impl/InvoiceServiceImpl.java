package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepo;
    private final BookingRepository bookingRepo;

    @Transactional
    @Override
    public Invoice issueForBooking(String bookingId) {
        var booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        // Không tạo trùng
        var existing = invoiceRepo.findByBooking_BookingId(bookingId);
        if (existing.isPresent()) return existing.get();

        // Tính tiền theo rule của bạn:
        // - Theo ngày: dueAmount = 50% * subtotal
        // - Theo tháng: dueAmount = tiền cọc = 1 tháng
        var subtotal = booking.getTotalPrice();// ví dụ đã tính sẵn
        var discount = BigDecimal.ZERO;          // nếu có mã giảm giá thì set
        var tax = BigDecimal.ZERO;               // tuỳ setup VAT
        var total = subtotal.subtract(discount).add(tax);

        BigDecimal dueAmount = computeDeposit(booking); // implement theo loại thuê

        var inv = Invoice.builder()
                .invoiceNo(generateInvoiceNo())
                .status(InvoiceStatus.ISSUED)
                .booking(booking)
                .tenantName(booking.getTenant().getFullName())
                .tenantEmail(booking.getTenant().getEmail())
                .tenantPhone(booking.getTenant().getPhoneNumber())
                .landlordName(booking.getProperty().getLandlord().getFullName())
                .landlordEmail(booking.getProperty().getLandlord().getEmail())
                .landlordPhone(booking.getProperty().getLandlord().getPhoneNumber())
                .propertyTitle(booking.getProperty().getTitle())
                .propertyAddressText(booking.getProperty().getAddress().getAddressFull())
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .total(total)
                .dueAmount(dueAmount)
                .issuedAt(LocalDateTime.now())
                .dueAt(booking.getCreatedAt().plusHours(24)) // ví dụ hạn TT 24h
                .paymentMethod("PAYOS")
                .itemsJson(buildItemsJson(booking)) // tuỳ bạn
                .build();

        return invoiceRepo.save(inv);
    }

    @Transactional
    public Invoice markPaidByWebhook(String invoiceNo, String paymentRef, BigDecimal paidAmount) {
        var inv = invoiceRepo.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        // Kiểm tra số tiền, tình trạng
        if (paidAmount.compareTo(inv.getDueAmount()) < 0)
            throw new IllegalStateException("Paid amount less than due amount");
        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaymentRef(paymentRef);
        inv.setPaidAt(LocalDateTime.now());
        return invoiceRepo.save(inv);
    }

    private BigDecimal computeDeposit(Booking booking) {
        // Ví dụ:
        // if (booking.getType() == DAILY) return booking.getTotalAmount().multiply(new BigDecimal("0.5"));
        // if (booking.getType() == MONTHLY) return booking.getMonthlyPrice(); // cọc 1 tháng
        return booking.getTotalPrice(); // tạm cho MVP
    }

    private String generateInvoiceNo() {
        // Pattern: ZR-YYYYMM-xxxxx tăng dần
        var prefix = "ZR-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now()) + "-";
        var max = invoiceRepo.findMaxInvoiceNoWithPrefix(prefix); // ví dụ: ZR-202509-000123
        int next = 1;
        if (max != null) {
            var tail = max.substring(prefix.length());
            next = Integer.parseInt(tail) + 1;
        }
        return prefix + String.format("%05d", next);
    }

    private String buildItemsJson(Booking b) {
        // Tối giản: 1 dòng mô tả
        // Có thể dùng Jackson ObjectMapper để serialize list item
        var map = Map.of(
                "items", List.of(Map.of(
                        "name", "Đặt phòng: " + b.getProperty().getTitle(),
                        "qty", 1,
                        "price", b.getTotalPrice(),
                        "amount", b.getTotalPrice()
                ))
        );
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
