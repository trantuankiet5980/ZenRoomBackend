package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;

@Component
@RequiredArgsConstructor
public class PropertyMapper {

    private final AddressMapper addressMapper;

    public PropertyDto toDto(Property entity) {
        if (entity == null) return null;

        return PropertyDto.builder()
                .propertyId(entity.getPropertyId())
                .landlordId(entity.getLandlord() != null ? entity.getLandlord().getUserId() : null)
                .propertyType(entity.getPropertyType() != null ? entity.getPropertyType().name() : null)
                .propertyName(entity.getPropertyName())
                .parentId(entity.getParent() != null ? entity.getParent().getPropertyId() : null)
                .roomTypeId(entity.getRoomType() != null ? entity.getRoomType().getRoomTypeId() : null)
                .roomNumber(entity.getRoomNumber())
                .floorNo(entity.getFloorNo())
                .area(entity.getArea())
                .capacity(entity.getCapacity())
                .parkingSlots(entity.getParkingSlots())
                .totalFloors(entity.getTotalFloors())
                .parkingCapacity(entity.getParkingCapacity())
                .price(entity.getPrice())
                .deposit(entity.getDeposit())
                .description(entity.getDescription())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .address(addressMapper.toDto(entity.getAddress())) // map sang AddressDto
                .build();
    }
    public Property toEntity(PropertyDto dto) {
        if (dto == null) return null;

        Property property = new Property();
        property.setPropertyId(dto.getPropertyId());
        property.setPropertyName(dto.getPropertyName());
        property.setRoomNumber(dto.getRoomNumber());
        property.setFloorNo(dto.getFloorNo());
        property.setArea(dto.getArea());
        property.setCapacity(dto.getCapacity());
        property.setParkingSlots(dto.getParkingSlots());
        property.setTotalFloors(dto.getTotalFloors());
        property.setParkingCapacity(dto.getParkingCapacity());
        property.setPrice(dto.getPrice());
        property.setDeposit(dto.getDeposit());
        property.setDescription(dto.getDescription());

        if (dto.getAddress() != null) {
            property.setAddress(addressMapper.toEntity(dto.getAddress()));
        }

        return property;
    }
}
