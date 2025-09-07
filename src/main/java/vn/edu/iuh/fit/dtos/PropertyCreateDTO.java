package vn.edu.iuh.fit.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyCreateDTO {
    private String landlordId;
    private String propertyType;
    private String propertyName;

    private String parentId;            // nếu là ROOM -> bắt buộc (id building)
    private String roomTypeId;          // nếu là ROOM -> nên có
    private String roomNumber;          // nếu là ROOM
    private Integer floorNo;            // nếu là ROOM
    private Double area;                // m2
    private Integer capacity;
    private Integer parkingSlots;

    private Integer totalFloors;        // nếu là BUILDING
    private Integer parkingCapacity;    // nếu là BUILDING

    private BigDecimal price;           // nếu là ROOM
    private BigDecimal deposit;         // nếu là ROOM

    private String description;

    private AddressCreateDTO address;
    private String addressId;
}
