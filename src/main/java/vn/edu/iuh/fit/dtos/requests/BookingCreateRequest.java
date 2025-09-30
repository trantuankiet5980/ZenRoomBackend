package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingCreateRequest {
    private String propertyId;
    private LocalDate checkInAt;   // bắt buộc
    private LocalDate checkOutAt;  // bắt buộc, > checkInAt
    private String note;
}
