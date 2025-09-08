package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Favorite}
 */
@Value
public class FavoriteDto implements Serializable {
    String favoriteId;
    UserDto tenant;
    PropertyDto property;
    LocalDateTime createdAt;
}