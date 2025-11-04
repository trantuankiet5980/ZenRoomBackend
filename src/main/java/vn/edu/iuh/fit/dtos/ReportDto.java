package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ReportReason;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Report}
 */
@Value
public class ReportDto implements Serializable {
    String reportId;
    String reporterId;
    String propertyId;
    ReportReason reason;
    String description;
    ReportStatus status;
    LocalDateTime createdAt;
}