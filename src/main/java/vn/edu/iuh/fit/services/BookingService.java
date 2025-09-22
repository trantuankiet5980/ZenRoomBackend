package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.requests.BookingCreateRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;

public interface BookingService {
    BookingDto createDaily(String tenantId, BookingCreateRequest req);    // tenant tạo PENDING
    BookingDto approve(String bookingId, String landlordId);              // landlord duyệt -> phát hành invoice 50%
    BookingDto reject(String bookingId, String landlordId);               // landlord từ chối
    BookingDto cancel(String bookingId, String tenantId);                 // tenant hủy
    BookingDto checkIn(String bookingId, String tenantId);                // check-in ngày nhận
    BookingDto checkOut(String bookingId, String tenantId);               // check-out ngày trả
    void handlePaymentWebhook(PaymentWebhookPayload payload);
}
