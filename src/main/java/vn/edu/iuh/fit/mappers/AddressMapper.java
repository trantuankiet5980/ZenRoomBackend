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
                .wardName(ward != null ? ward.getName_with_type() : null)
                .districtId((ward != null && ward.getDistrict() != null) ? ward.getDistrict().getCode() : null)
                .districtName((ward != null && ward.getDistrict() != null) ? ward.getDistrict().getName_with_type() : null)
                .provinceId((ward != null && ward.getDistrict() != null && ward.getDistrict().getProvince() != null)
                        ? ward.getDistrict().getProvince().getCode() : null)
                .provinceName((ward != null && ward.getDistrict() != null && ward.getDistrict().getProvince() != null)
                        ? ward.getDistrict().getProvince().getName_with_type() : null)
                .street(entity.getStreet())
                .houseNumber(entity.getHouseNumber())
                .addressFull(entity.getAddressFull())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }

    public Address toEntity(AddressDto dto, Ward ward) {
        if (dto == null) return null;

        Address address = Address.builder()
                .addressId(dto.getAddressId())
                .ward(ward)
                .street(dto.getStreet())
                .houseNumber(dto.getHouseNumber())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();

        // Bổ sung district & province từ ward
        if (ward != null) {
            address.setDistrict(ward.getDistrict());
            if (ward.getDistrict() != null) {
                address.setProvince(ward.getDistrict().getProvince());
            }
        }

        // Luôn generate full address
        if (dto.getAddressFull() != null) {
            address.setAddressFull(dto.getAddressFull());
        } else {
            address.generateAddressFull();
        }

        return address;
    }

    public Address toEntity(AddressDto dto) {
        return toEntity(dto, null);
    }

    public void updateEntity(Address entity, AddressDto dto, Ward ward) {
        if (dto == null || entity == null) return;

        if (ward != null) {
            entity.setWard(ward);
            entity.setDistrict(ward.getDistrict());
            if (ward.getDistrict() != null) {
                entity.setProvince(ward.getDistrict().getProvince());
            }
        }

        if (dto.getStreet() != null) entity.setStreet(dto.getStreet());
        if (dto.getHouseNumber() != null) entity.setHouseNumber(dto.getHouseNumber());
        if (dto.getLatitude() != null) entity.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) entity.setLongitude(dto.getLongitude());

        if (dto.getAddressFull() != null && !dto.getAddressFull().isBlank()) {
            entity.setAddressFull(dto.getAddressFull());
        } else {
            entity.generateAddressFull();
        }
    }

    public void updateEntity(Address entity, AddressDto dto) {
        // fallback ward từ entity
        updateEntity(entity, dto, entity.getWard());
    }
}
