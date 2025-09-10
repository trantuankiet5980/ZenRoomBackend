package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.ApartmentCategory;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Property}
 */
@Value
public class PropertyDto implements Serializable {
    String propertyId;
    PropertyType propertyType;
    UserDto landlord;
    AddressDto address;
    String title;
    String description;
    Double area;
    Integer capacity;
    Integer parkingSlots;
    BigDecimal price;
    BigDecimal deposit;
    String buildingName;
    ApartmentCategory apartmentCategory;
    Integer bedrooms;
    Integer bathrooms;
    String roomNumber;
    Integer floorNo;
    List<PropertyFurnishingDto> furnishings;
    List<PropertyMediaDto> media;
    PostStatus postStatus;
    String rejectedReason;
    LocalDateTime publishedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}