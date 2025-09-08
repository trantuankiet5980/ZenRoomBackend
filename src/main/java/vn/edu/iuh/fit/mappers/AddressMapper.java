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
                .countryCode(entity.getCountryCode())
                .province(entity.getProvince())
                .district(entity.getDistrict())
                .ward(entity.getWard())
                .street(entity.getStreet())
                .houseNumber(entity.getHouseNumber())
                .postalCode(entity.getPostalCode())
                .addressFull(entity.getAddressFull())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }

    public Address toEntity(AddressDto dto) {
        if (dto == null) return null;

        Address address = new Address();
        address.setAddressId(dto.getAddressId());
        address.setCountryCode(dto.getCountryCode());
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setWard(dto.getWard());
        address.setStreet(dto.getStreet());
        address.setHouseNumber(dto.getHouseNumber());
        address.setPostalCode(dto.getPostalCode());
        address.setAddressFull(dto.getAddressFull());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());
        return address;
    }
}
