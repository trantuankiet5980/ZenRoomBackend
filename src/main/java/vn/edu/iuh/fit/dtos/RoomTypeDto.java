package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.RoomType}
 */
@Value
public class RoomTypeDto implements Serializable {
    String roomTypeId;
    String typeName;
}