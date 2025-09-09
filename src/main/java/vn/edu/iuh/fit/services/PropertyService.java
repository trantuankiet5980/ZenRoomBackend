package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.util.Optional;

public interface PropertyService {
    Property create(PropertyDto dto);
    Optional<PropertyDto> getById(String id);

    Page<PropertyDto> list(String landlordId, String postStatus, String type, String keyword, Pageable pageable);

    Property update(String id, PropertyDto dto);             // luôn đưa về PENDING
    void changeStatus(String id, PostStatus status, String rejectedReason);
    void delete(String id);
}
