package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingCreateRequest {
    private String propertyId;
    private LocalDateTime checkInAt;   // bắt buộc
    private LocalDateTime checkOutAt;  // bắt buộc, > checkInAt
    private String note;
}
