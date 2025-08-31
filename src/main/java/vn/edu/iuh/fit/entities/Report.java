package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Reports")
public class Report {
    @Id @Column(name="report_id", columnDefinition="CHAR(36)") String reportId;
    @PrePersist
    private void prePersist() {
        if (this.reportId == null) {
            this.reportId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }
    @ManyToOne @JoinColumn(name="reporter_id") private User reporter;
    @ManyToOne @JoinColumn(name="reported_id") private User reported;
    @ManyToOne @JoinColumn(name="property_id") private Property property;
    @ManyToOne @JoinColumn(name="booking_id") private Booking booking;
    private String reason;
    @Enumerated(EnumType.STRING) private ReportStatus status;
    private LocalDateTime createdAt;
}
