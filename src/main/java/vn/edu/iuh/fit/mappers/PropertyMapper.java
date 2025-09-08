package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.entities.Property;

import java.util.stream.Collectors;

@Component
public class PropertyMapper {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PropertyFurnishingMapper furnishingMapper;
    private final PropertyAmenityMapper amenityMapper;
    private final PropertyMediaMapper mediaMapper;

    public PropertyMapper(UserMapper userMapper,
                          AddressMapper addressMapper,
                          PropertyFurnishingMapper furnishingMapper,
                          PropertyAmenityMapper amenityMapper,
                          PropertyMediaMapper mediaMapper) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.furnishingMapper = furnishingMapper;
        this.amenityMapper = amenityMapper;
        this.mediaMapper = mediaMapper;
    }

    /** Entity -> DTO */
    public PropertyDto toDto(Property entity) {
        if (entity == null) return null;

        return new PropertyDto(
                entity.getPropertyId(),
                entity.getPropertyType(),
                userMapper.toDto(entity.getLandlord()),
                addressMapper.toDto(entity.getAddress()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getArea(),
                entity.getPrice(),
                entity.getDeposit(),
                entity.getBuildingName(),
                entity.getApartmentCategory(),
                entity.getBedrooms(),
                entity.getBathrooms(),
                entity.getRoomNumber(),
                entity.getFloorNo(),
                entity.getFurnishings() != null ?
                        entity.getFurnishings().stream().map(furnishingMapper::toDto).collect(Collectors.toList()) : null,
                entity.getAmenities() != null ?
                        entity.getAmenities().stream().map(amenityMapper::toDto).collect(Collectors.toList()) : null,

                entity.getMedia() != null ?
                        entity.getMedia().stream().map(mediaMapper::toDto).collect(Collectors.toList()) : null,
                entity.getPostStatus(),
                entity.getRejectedReason(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /** DTO -> Entity */
    public Property toEntity(PropertyDto dto) {
        if (dto == null) return null;

        Property entity = new Property();
        entity.setPropertyId(dto.getPropertyId());
        entity.setPropertyType(dto.getPropertyType());
        entity.setLandlord(userMapper.toEntity(dto.getLandlord()));
        entity.setAddress(addressMapper.toEntity(dto.getAddress()));
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setArea(dto.getArea());
        entity.setPrice(dto.getPrice());
        entity.setDeposit(dto.getDeposit());
        entity.setBuildingName(dto.getBuildingName());
        entity.setApartmentCategory(dto.getApartmentCategory());
        entity.setBedrooms(dto.getBedrooms());
        entity.setBathrooms(dto.getBathrooms());
        entity.setRoomNumber(dto.getRoomNumber());
        entity.setFloorNo(dto.getFloorNo());
        entity.setPostStatus(dto.getPostStatus());
        entity.setRejectedReason(dto.getRejectedReason());
        entity.setPublishedAt(dto.getPublishedAt());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        // Furnishings
        if (dto.getFurnishings() != null) {
            entity.setFurnishings(dto.getFurnishings().stream()
                    .map(furnishingMapper::toEntity)
                    .collect(Collectors.toList()));
        }

        // Amenities
        if (dto.getAmenities() != null) {
            entity.setAmenities(dto.getAmenities().stream()
                    .map(amenityMapper::toEntity)
                    .collect(Collectors.toList()));
        }


        // Media
        if (dto.getMedia() != null) {
            entity.setMedia(dto.getMedia().stream()
                    .map(mediaMapper::toEntity)
                    .collect(Collectors.toList()));
        }

        return entity;
    }
}
