package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.entities.enums.PostStatus;

public interface RealtimeNotificationService {
    //Thong bao cho admin khi co bai dang moi
    void notifyAdminsPropertyCreated(PropertyDto property);
    //Thong bao cho admin khi bai dang bi cap nhat
    void notifyAdminsPropertyUpdated(PropertyDto p);

    // Thong bao khi bai dang thay doi trang thai
    void notifyAdminsPropertyStatusChanged(PropertyDto p, PostStatus status, String rejectedReason); // NEW

    void notifyTenantBookingApproved(BookingDto booking);

    void notifyLandlordBookingCreated(BookingDto booking);

    void notifyPaymentStatusChanged(BookingDto booking, Invoice invoice, boolean success);

    void notifyBookingCheckedIn(BookingDto booking);

    void notifyBookingCheckedOut(BookingDto booking);

    void notifyRefundProcessed(BookingDto booking, Invoice invoice);

    void notifyBookingCancelledByTenant(BookingDto booking, Invoice invoice);

    NotificationDto createAndPush(
            User target,
            String title,
            String message,
            NotificationType type,
            String redirectUrl
    );
}
