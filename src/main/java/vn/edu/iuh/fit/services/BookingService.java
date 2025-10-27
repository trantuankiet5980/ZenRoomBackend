package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.requests.BookingCreateRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentConfirmationRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    BookingDto createDaily(String tenantId, BookingCreateRequest req);
    BookingDto cancel(String bookingId, String tenantId);
    BookingDto approve(String bookingId, String landlordId);
    BookingDto checkIn(String bookingId, String tenantId);                // check-in ngày nhận
    BookingDto checkOut(String bookingId, String tenantId);               // check-out ngày trả
    BookingDto getOne(String bookingId, String userId);
    void handlePaymentWebhook(PaymentWebhookPayload payload);
    void confirmVirtualPayment(PaymentConfirmationRequest request);
    List<LocalDate> getBookedDates(String propertyId);
}
