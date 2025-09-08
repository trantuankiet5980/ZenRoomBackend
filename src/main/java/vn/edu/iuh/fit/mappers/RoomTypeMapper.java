package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.RoomTypeDto;
import vn.edu.iuh.fit.entities.RoomType;

@Component
public class RoomTypeMapper {

    /** Entity -> DTO */
    public RoomTypeDto toDto(RoomType e) {
        if (e == null) return null;
        return new RoomTypeDto(
                e.getRoomTypeId(),
                e.getTypeName()
        );
    }

    /** DTO -> Entity */
    public RoomType toEntity(RoomTypeDto d) {
        if (d == null) return null;
        return RoomType.builder()
                .roomTypeId(d.getRoomTypeId())
                .typeName(d.getTypeName())
                .build();
    }
}
