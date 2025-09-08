package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyAmenityDto;
import vn.edu.iuh.fit.entities.PropertyAmenity;

@Component
public class PropertyAmenityMapper {

    public PropertyAmenityDto toDto(PropertyAmenity entity) {
        if (entity == null) return null;

        return new PropertyAmenityDto(
                entity.getId()
        );
    }

    public PropertyAmenity toEntity(PropertyAmenityDto dto) {
        if (dto == null) return null;

        PropertyAmenity entity = new PropertyAmenity();
        entity.setId(dto.getId());
        // property và amenity sẽ được set trong service để tránh vòng lặp
        return entity;
    }
}
