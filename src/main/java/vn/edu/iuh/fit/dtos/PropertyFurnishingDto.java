package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.PropertyFurnishing}
 */
@Value
public class PropertyFurnishingDto implements Serializable {
    String id;
    String furnishingId;
    String furnishingName;
    Integer quantity;
}