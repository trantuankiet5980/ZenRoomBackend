package vn.edu.iuh.fit.entities.enums;

public enum PaymentStatus {
    PENDING,    // Đã tạo yêu cầu, chờ user thanh toán
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    REFUNDED    // Đã hoàn tiền
}
