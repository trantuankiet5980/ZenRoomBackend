package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.PropertyStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {
    @Id
    @Column(name="property_id", columnDefinition="CHAR(36)")
    private String propertyId;
    @PrePersist
    void pre(){ if(propertyId==null) propertyId= UUID.randomUUID().toString(); }

    @Enumerated(EnumType.STRING) private PropertyType propertyType;
    @ManyToOne @JoinColumn(name="parent_id") private Property parent;
    @ManyToOne @JoinColumn(name="landlord_id") private User landlord;
    private String propertyName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;

    private Integer totalFloors;
    private Integer parkingCapacity;

    @ManyToOne @JoinColumn(name="room_type_id") private RoomType roomType;
    private String roomNumber;

    private Integer floorNo;
    private Double area;
    private Integer capacity;
    private Integer parkingSlots;
    private BigDecimal price;
    private BigDecimal deposit;
    @Enumerated(EnumType.STRING) private PropertyStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy="property") private List<PropertyMedia> media = new ArrayList<>();
}
