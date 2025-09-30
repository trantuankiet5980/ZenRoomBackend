package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @Column(name = "contract_id", columnDefinition = "CHAR(36)")
    private String contractId;

    @PrePersist
    void prePersist() {
        if (contractId == null) contractId = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (contractStatus == null) contractStatus = ContractStatus.PENDING_REVIEW;
    }

    // Quan hệ 1–1 với Booking
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    private Booking booking;

    @Column(name = "tenant_name", length = 100, nullable = false)
    private String tenantName;

    @Column(name = "tenant_phone", length = 15, nullable = false)
    private String tenantPhone;

    @Column(name = "tenant_cccd_front", length = 255)
    private String tenantCccdFront; // ảnh mặt trước

    @Column(name = "tenant_cccd_back", length = 255)
    private String tenantCccdBack;  // ảnh mặt sau

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "building_name", length = 255)
    private String buildingName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", length = 30)
    private ContractStatus contractStatus;

    @OneToMany(mappedBy = "contract", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractService> services = new ArrayList<>();

    @Column(name = "rent_price", precision = 12, scale = 2)
    private BigDecimal rentPrice;

    @Column(name = "deposit", precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "billing_start_date")
    private LocalDate billingStartDate;

    @Column(name = "payment_due_day")
    private Integer paymentDueDay; // ngày thanh toán hàng tháng

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
