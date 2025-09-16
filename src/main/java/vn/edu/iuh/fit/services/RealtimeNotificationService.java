package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;

public interface RealtimeNotificationService {
    void notifyAdminsPropertyCreated(PropertyDto property);
}
