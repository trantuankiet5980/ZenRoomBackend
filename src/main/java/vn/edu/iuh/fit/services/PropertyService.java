package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PropertyService {
    Property create(PropertyDto dto);
    Optional<PropertyDto> getById(String id);

    Page<PropertyDto> list(String landlordId, String postStatus, String type, String keyword,
                           String provinceCode, String districtCode, LocalDate createdFrom, LocalDate createdTo,
                           Pageable pageable);

    Property update(String id, PropertyDto dto);             // luôn đưa về PENDING
    void changeStatus(String id, PostStatus status, String rejectedReason);
    void delete(String id);

    Page<PropertyDto> search(
            String userId,          // null nếu chưa đăng nhập → không lưu lịch sử
            String keyword,
            Integer priceMin, Integer priceMax,
            Integer areaMin, Integer areaMax,
            String apartmentCategory,  // CHUNG_CU | DUPLEX | PENTHOUSE ...
            Integer floorNo,
            String roomNumber,
            Integer bathrooms, Integer bedrooms,
            Integer capacity, Integer parkingSlots,
            String buildingName, String propertyType, // nếu bạn có enum PropertyType
            String provinceCode, String districtCode,
            int page, int size
    );

    //AI Recommendation
    List<PropertyDto> recommendProperties(String propertyId, int limit);
}
