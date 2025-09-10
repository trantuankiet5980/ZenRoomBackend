package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.entities.Address;

@Component
public class AddressMapper {

    public AddressDto toDto(Address entity) {
        if (entity == null) return null;

        return AddressDto.builder()
                .addressId(entity.getAddressId())
                .province(entity.getProvince())
                .district(entity.getDistrict())
                .ward(entity.getWard())
                .street(entity.getStreet())
                .houseNumber(entity.getHouseNumber())
                .addressFull(entity.getAddressFull())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }

    public Address toEntity(AddressDto dto) {
        if (dto == null) return null;

        Address address = new Address();
        address.setAddressId(dto.getAddressId());
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setWard(dto.getWard());
        address.setStreet(dto.getStreet());
        address.setHouseNumber(dto.getHouseNumber());
        address.setAddressFull(dto.getAddressFull());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        return address;
    }

    public void updateEntity(Address entity, AddressDto dto) {
        if (dto == null || entity == null) return;

        if (dto.getProvince() != null) entity.setProvince(dto.getProvince());
        if (dto.getDistrict() != null) entity.setDistrict(dto.getDistrict());
        if (dto.getWard() != null) entity.setWard(dto.getWard());
        if (dto.getStreet() != null) entity.setStreet(dto.getStreet());
        if (dto.getHouseNumber() != null) entity.setHouseNumber(dto.getHouseNumber());
        if (dto.getAddressFull() != null) entity.setAddressFull(dto.getAddressFull());
        if (dto.getLatitude() != null) entity.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) entity.setLongitude(dto.getLongitude());
    }
}
