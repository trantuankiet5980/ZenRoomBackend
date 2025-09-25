package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Ward;

@Component
public class AddressMapper {

    public AddressDto toDto(Address entity) {
        if (entity == null) return null;

        Ward ward = entity.getWard();

        return AddressDto.builder()
                .addressId(entity.getAddressId())
                .wardId(ward != null ? ward.getCode() : null)
                .wardName(ward != null ? ward.getName() : null)
                .districtId(ward != null ? ward.getDistrict().getCode() : null)
                .districtName(ward != null ? ward.getDistrict().getName() : null)
                .provinceId(ward != null ? ward.getDistrict().getProvince().getCode() : null)
                .provinceName(ward != null ? ward.getDistrict().getProvince().getName() : null)
                .street(entity.getStreet())
                .houseNumber(entity.getHouseNumber())
                .addressFull(entity.getAddressFull())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }

    public Address toEntity(AddressDto dto, Ward ward) {
        if (dto == null) return null;

        return Address.builder()
                .addressId(dto.getAddressId())
                .ward(ward) // đã load từ DB ở service
                .street(dto.getStreet())
                .houseNumber(dto.getHouseNumber())
                .addressFull(dto.getAddressFull())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public void updateEntity(Address entity, AddressDto dto, Ward ward) {
        if (dto == null || entity == null) return;

        if (ward != null) entity.setWard(ward);
        if (dto.getStreet() != null) entity.setStreet(dto.getStreet());
        if (dto.getHouseNumber() != null) entity.setHouseNumber(dto.getHouseNumber());
        if (dto.getAddressFull() != null) entity.setAddressFull(dto.getAddressFull());
        if (dto.getLatitude() != null) entity.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) entity.setLongitude(dto.getLongitude());
    }

    public Address toEntity(AddressDto dto) {
        return toEntity(dto, null);
    }

    public void updateEntity(Address entity, AddressDto dto) {
        updateEntity(entity, dto, null);
    }

}
