package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Amenity}
 */
@Value
public class AmenityDto implements Serializable {
    String amenityId;
    String amenityName;
    List<PropertyAmenityDto> properties;
}