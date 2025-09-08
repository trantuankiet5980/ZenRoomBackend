package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Furnishings}
 */
@Value
public class FurnishingsDto implements Serializable {
    String furnishingId;
    String furnishingName;
    List<PropertyFurnishingDto> properties;
}