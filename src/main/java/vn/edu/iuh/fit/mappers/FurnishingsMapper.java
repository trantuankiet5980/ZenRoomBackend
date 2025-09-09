package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.FurnishingsDto;
import vn.edu.iuh.fit.dtos.PropertyFurnishingDto;
import vn.edu.iuh.fit.entities.Furnishings;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FurnishingsMapper {

    private final PropertyFurnishingMapper propertyFurnishingMapper;

    public FurnishingsDto toDto(Furnishings entity) {
        if (entity == null) return null;

        List<PropertyFurnishingDto> propertyDtos = null;
        if (entity.getProperties() != null) {
            propertyDtos = entity.getProperties().stream()
                    .map(propertyFurnishingMapper::toDto)
                    .collect(Collectors.toList());
        }

        return new FurnishingsDto(
                entity.getFurnishingId(),
                entity.getFurnishingName(),
                propertyDtos
        );
    }

    public Furnishings toEntity(FurnishingsDto dto) {
        if (dto == null) return null;

        Furnishings entity = new Furnishings();
        entity.setFurnishingId(dto.getFurnishingId());
        entity.setFurnishingName(dto.getFurnishingName());
        // propertyFurnishings sẽ được gán trong service
        return entity;
    }
}
