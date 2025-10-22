package vn.edu.iuh.fit.entities.enums;

public enum InvoiceStatus {
    DRAFT,      // Booking vừa tạo, chưa phát hành hóa đơn
    ISSUED,     // Đã phát hành, chờ thanh toán (khi duyệt booking hoặc đến hạn)
    PAID,       // Đã thanh toán (qua webhook PayOS)
    REFUND_PENDING, // Đang chờ hoàn tiền thủ công
    REFUNDED,   // Đã hoàn tiền
    VOID        // Hủy bỏ (booking hủy/sai)
}
