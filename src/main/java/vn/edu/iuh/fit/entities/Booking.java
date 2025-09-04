package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import vn.edu.iuh.fit.entities.enums.BookingStatus;

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

    @ManyToOne @JoinColumn(name="property_id") private Property property;
    @ManyToOne @JoinColumn(name="tenant_id") private User tenant;
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING) private BookingStatus bookingStatus;
    private BigDecimal totalPrice;
    @ManyToOne @JoinColumn(name="discount_code_id") private DiscountCode discountCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Contract contract;
}
