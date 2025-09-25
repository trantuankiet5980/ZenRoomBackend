package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.dtos.CoordinatesDTO;

import java.util.List;

public interface AddressService {
    CoordinatesDTO getCoordinatesFromFullAddress(String fullAddress);
    String getAddressFromCoordinates(double lat, double lng);

    AddressDto save(AddressDto dto);
    AddressDto update(String id, AddressDto dto);
    AddressDto getById(String id);
    List<AddressDto> getAll();
    void delete(String id);
}
