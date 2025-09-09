package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyFurnishingDto;
import vn.edu.iuh.fit.entities.PropertyFurnishing;

@Component
public class PropertyFurnishingMapper {

    public PropertyFurnishingDto toDto(PropertyFurnishing entity) {
        if (entity == null) return null;
        String fid = (entity.getFurnishing() != null) ? entity.getFurnishing().getFurnishingId() : null;
        String fname = (entity.getFurnishing() != null) ? entity.getFurnishing().getFurnishingName() : null;

        return new PropertyFurnishingDto(
                entity.getId(),
                fid,
                entity.getQuantity()
        );
    }

    public PropertyFurnishing toEntity(PropertyFurnishingDto dto) {
        if (dto == null) return null;

        PropertyFurnishing entity = new PropertyFurnishing();
        entity.setId(dto.getId());
        entity.setQuantity(dto.getQuantity());

        // property và furnishing sẽ được gán trong service để tránh vòng lặp/lazy load
        return entity;
    }
}
