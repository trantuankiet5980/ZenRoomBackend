package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;

public interface PropertyService {
    Property create(PropertyDto dto);
}
