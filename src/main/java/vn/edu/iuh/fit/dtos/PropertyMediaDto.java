package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.io.Serializable;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.PropertyMedia}
 */
@Value
public class PropertyMediaDto implements Serializable {
    String mediaId;
    String propertyId;
    MediaType mediaType;
    String url;
    String posterUrl;
    Integer sortOrder;
    Boolean isCover;
}