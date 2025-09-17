package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.enums.PostStatus;

public interface RealtimeNotificationService {
    //Thong bao cho admin khi co bai dang moi
    void notifyAdminsPropertyCreated(PropertyDto property);
    //Thong bao cho admin khi bai dang bi cap nhat
    void notifyAdminsPropertyUpdated(PropertyDto p);

    // Thong bao khi bai dang thay doi trang thai
    void notifyAdminsPropertyStatusChanged(PropertyDto p, PostStatus status, String rejectedReason); // NEW
}
