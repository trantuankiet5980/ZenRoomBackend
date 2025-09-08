package vn.edu.iuh.fit.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.PropertyStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Property}
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)          // không trả field null ra JSON
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyDto implements Serializable {
    private String propertyId;       // dùng khi trả về / update
    private String landlordId;
    private String propertyType;
    private String propertyName;

    private String parentId;
    private String roomTypeId;
    private String roomNumber;
    private Integer floorNo;
    private Double area;
    private Integer capacity;
    private Integer parkingSlots;

    private Integer totalFloors;
    private Integer parkingCapacity;

    private BigDecimal price;
    private BigDecimal deposit;

    private String description;

    private AddressDto address;
    private String addressId;

    private String status;           // PostStatus / PropertyStatus
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}