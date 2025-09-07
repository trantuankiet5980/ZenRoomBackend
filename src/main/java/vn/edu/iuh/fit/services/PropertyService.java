package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyCreateDTO;
import vn.edu.iuh.fit.entities.Property;

public interface PropertyService {
    Property create(PropertyCreateDTO dto);
}
