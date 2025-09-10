package vn.edu.iuh.fit.entities.enums;

public enum InvoiceStatus {
    DRAFT,      // tạo nháp (booking vừa tạo)
    ISSUED,     // đã phát hành (khi xác nhận booking/đến hạn thanh toán)
    PAID,       // đã thanh toán (webhook xác nhận)
    VOID,       // hủy hóa đơn (booking hủy hoặc sai)
    REFUNDED    // đã hoàn tiền (tạo credit note hoặc dòng âm)
}
