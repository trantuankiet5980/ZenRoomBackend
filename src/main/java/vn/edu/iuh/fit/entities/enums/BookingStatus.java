package vn.edu.iuh.fit.entities.enums;

public enum BookingStatus {
    PENDING_PAYMENT,            // Người thuê vừa tạo booking, đang chờ thanh toán
    AWAITING_LANDLORD_APPROVAL, // Đã thanh toán, chờ chủ nhà duyệt hợp đồng
    APPROVED,                   // Chủ nhà đã duyệt, hợp đồng có hiệu lực
    CANCELLED,                  // Người thuê hoặc chủ nhà hủy
    CHECKED_IN,                 // Người thuê đã check-in
    COMPLETED                   // Kết thúc lưu trú, checkout
}
