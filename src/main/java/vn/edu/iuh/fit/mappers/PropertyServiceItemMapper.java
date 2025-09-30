package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyServiceItemDto;
import vn.edu.iuh.fit.entities.PropertyServiceItem;

@Component
public class PropertyServiceItemMapper {
    public PropertyServiceItemDto toDto(PropertyServiceItem entity) {
        if (entity == null) {
            return null;
        }
        return new PropertyServiceItemDto(
                entity.getId(),
                entity.getServiceName(),
                entity.getFee(),
                entity.getChargeBasis(),
                entity.getIsIncluded(),
                entity.getNote()
        );
    }

    public PropertyServiceItem toEntity(PropertyServiceItemDto dto) {
        if (dto == null) {
            return null;
        }
        return PropertyServiceItem.builder()
                .id(dto.getId())
                .serviceName(dto.getServiceName())
                .fee(dto.getFee())
                .chargeBasis(dto.getChargeBasis())
                .isIncluded(dto.getIsIncluded())
                .note(dto.getNote())
                .build();
    }
}
