package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.AmenityDto;
import vn.edu.iuh.fit.dtos.PropertyAmenityDto;
import vn.edu.iuh.fit.entities.Amenity;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AmenityMapper {

    private final PropertyAmenityMapper propertyAmenityMapper;

    public AmenityDto toDto(Amenity entity) {
        if (entity == null) return null;

        List<PropertyAmenityDto> propertyDtos = null;
        if (entity.getProperties() != null) {
            propertyDtos = entity.getProperties().stream()
                    .map(propertyAmenityMapper::toDto)
                    .collect(Collectors.toList());
        }

        return new AmenityDto(
                entity.getAmenityId(),
                entity.getAmenityName(),
                propertyDtos
        );
    }

    public Amenity toEntity(AmenityDto dto) {
        if (dto == null) return null;

        Amenity entity = new Amenity();
        entity.setAmenityId(dto.getAmenityId());
        entity.setAmenityName(dto.getAmenityName());
        // propertyAmenities sẽ được set ở service khi tạo/sửa
        return entity;
    }
}
