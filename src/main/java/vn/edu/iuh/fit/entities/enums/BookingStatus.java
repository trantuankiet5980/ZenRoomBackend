package vn.edu.iuh.fit.entities.enums;

public enum BookingStatus {
    PENDING,      // Người thuê vừa tạo booking, chờ chủ nhà duyệt
    APPROVED,     // Chủ nhà duyệt (sẵn sàng thanh toán / check-in)
    REJECTED,     // Chủ nhà từ chối
    CANCELLED,    // Người thuê hủy (có thể mất cọc hoặc hoàn tiền tùy rule)
    CHECKED_IN,   // Người thuê đã check-in
    COMPLETED     // Kết thúc lưu trú, checkout
}
