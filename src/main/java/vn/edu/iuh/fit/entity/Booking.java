package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

import vn.edu.iuh.fit.entity.enums.BookingStatus;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Bookings")
public class Booking {
    @Id
    @Column(name = "booking_id", columnDefinition = "CHAR(36)")
    private String bookingId;

    @PrePersist
    void prePersist() {
        if (this.bookingId == null) this.bookingId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "user_id")
    private User tenant;

    @Column(name = "start_date") private java.time.LocalDate startDate;
    @Column(name = "end_date") private java.time.LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", length = 20)
    private BookingStatus bookingStatus;

    @Column(name = "total_price", precision = 12, scale = 2)
    private java.math.BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id", referencedColumnName = "code_id")
    private DiscountCode discountCode;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Payment payment;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    @Builder.Default private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    @Builder.Default private List<DiscountCodeUsage> couponUsages = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    @Builder.Default private List<Report> reports = new ArrayList<>();
}
