package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ReportStatus;

import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Reports")
public class Report {
    @Id
    @Column(name = "report_id", columnDefinition = "CHAR(36)")
    private String reportId;

    @PrePersist
    void prePersist() {
        if (this.reportId == null) this.reportId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", referencedColumnName = "user_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", referencedColumnName = "user_id")
    private User reported;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    private Booking booking;

    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReportStatus status;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
