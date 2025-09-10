package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice",
        indexes = {
                @Index(name="ix_invoice_no", columnList = "invoice_no", unique = true),
                @Index(name="ix_invoice_booking", columnList = "booking_id")
        })
public class Invoice {
    @Id
    @Column(name = "invoice_id", columnDefinition = "CHAR(36)")
    private String invoiceId;

    @PrePersist
    void pre() {
        if (invoiceId == null) invoiceId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    @PreUpdate void upd(){ updatedAt = LocalDateTime.now(); }

    // Số hóa đơn hiển thị cho người dùng: ZR-202509-000123
    @Column(name = "invoice_no", length = 32, unique = true, nullable = false)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private InvoiceStatus status;

    // Link 1-1 tới booking
    @OneToOne(optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Ảnh chụp dữ liệu để bảo toàn pháp lý (thông tin có thể đổi về sau)
    @Column(length = 100) private String tenantName;
    @Column(length = 100) private String tenantEmail;
    @Column(length = 15)  private String tenantPhone;

    @Column(length = 100) private String landlordName;
    @Column(length = 100) private String landlordEmail;
    @Column(length = 15)  private String landlordPhone;

    // Thông tin phòng tại thời điểm phát hành
    @Column(length = 255) private String propertyTitle;
    @Column(length = 255) private String propertyAddressText;

    // Số tiền
    @Column(precision = 14, scale = 2) private BigDecimal subtotal;  // giá gốc
    @Column(precision = 14, scale = 2) private BigDecimal discount;  // giảm giá (nếu có)
    @Column(precision = 14, scale = 2) private BigDecimal tax;       // VAT nếu áp dụng
    @Column(precision = 14, scale = 2, nullable = false) private BigDecimal total;     // phải thu

    // Cấu hình “cọc 50% theo ngày” hoặc “cọc 1 tháng” => thể hiện ở dueAmount
    @Column(precision = 14, scale = 2, nullable = false) private BigDecimal dueAmount; // số phải thanh toán kỳ này (VD: tiền cọc)

    // Thanh toán
    @Column(length = 32) private String paymentMethod; // "PAYOS", "CASH", ...
    @Column(length = 64) private String paymentRef;    // mã giao dịch từ PayOS / bank
    private LocalDateTime paidAt;

    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // (Tuỳ chọn) Lưu JSON line items để in hóa đơn
    @Lob @Column(columnDefinition = "TEXT")
    private String itemsJson; // [{name, qty, price, amount}]
}
