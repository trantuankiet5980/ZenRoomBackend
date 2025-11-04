package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Value
public class ReportListItemDto implements Serializable {
    String reportId;
    String reporterId;
    String reporterName;
    String propertyId;
    String propertyTitle;
    String reason;
    String description;
    ReportStatus status;
    LocalDateTime createdAt;
}
