package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ReportReason;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.time.*;
import java.util.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity @Table(name = "Reports")
public class Report {
    @Id
    @Column(name = "report_id", columnDefinition = "CHAR(36)")
    String reportId;
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
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;
    private LocalDateTime createdAt;
}
