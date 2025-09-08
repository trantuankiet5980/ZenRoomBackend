package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.DiscountCodeDto;
import vn.edu.iuh.fit.entities.DiscountCode;

@Component
public class DiscountCodeMapper {

    public DiscountCodeDto toDto(DiscountCode entity) {
        if (entity == null) return null;
        return new DiscountCodeDto(
                entity.getCodeId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getUsageLimit(),
                entity.getUsedCount(),
                entity.getStatus()
        );
    }

    public DiscountCode toEntity(DiscountCodeDto dto) {
        if (dto == null) return null;
        DiscountCode entity = new DiscountCode();
        entity.setCodeId(dto.getCodeId());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());
        entity.setDiscountType(dto.getDiscountType());
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        entity.setUsageLimit(dto.getUsageLimit());
        entity.setUsedCount(dto.getUsedCount());
        entity.setStatus(dto.getStatus());
        return entity;
    }
}
