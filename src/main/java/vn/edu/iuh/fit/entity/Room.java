package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


import vn.edu.iuh.fit.entity.enums.RoomStatus;
import vn.edu.iuh.fit.entity.enums.RoomType;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Rooms")
public class Room {
    @Id
    @Column(name = "room_id", columnDefinition = "CHAR(36)")
    private String roomId;

    @PrePersist
    void prePersist() {
        if (this.roomId == null) this.roomId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", referencedColumnName = "user_id")
    private User landlord;

    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", length = 255)
    private String address;

    private Double latitude;
    private Double longitude;

    private java.math.BigDecimal price;
    private java.math.BigDecimal deposit;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType roomType;

    @Column(name = "max_guests") private Integer maxGuests;
    @Column(name = "num_bedrooms") private Integer numBedrooms;
    @Column(name = "num_bathrooms") private Integer numBathrooms;
    @Column(name = "area") private Double area;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomStatus status;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<RoomImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<RoomAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @Builder.Default private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @Builder.Default private List<Report> reports = new ArrayList<>();

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @Builder.Default private List<Conversation> conversations = new ArrayList<>();

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @Builder.Default private List<Favorite> favorites = new ArrayList<>();
}
