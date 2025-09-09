package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;

import java.util.List;

public interface PropertyService {
    Property create(PropertyDto dto);
    //get ds property by landlordId
    List<Property> getByLandlordId(String landlordId);
    List<Property> getAll();
}
