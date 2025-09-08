package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Report}
 */
@Value
public class ReportDto implements Serializable {
    String reportId;
    UserDto reporter;
    UserDto reported;
    PropertyDto property;
    BookingDto booking;
    String reason;
    ReportStatus status;
    LocalDateTime createdAt;
}