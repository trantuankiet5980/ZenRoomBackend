package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.PropertyFurnishingDto;
import vn.edu.iuh.fit.entities.*;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.mappers.PropertyMapper;
import vn.edu.iuh.fit.repositories.*;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.PropertyService;
import vn.edu.iuh.fit.services.RealtimeNotificationService;
import vn.edu.iuh.fit.services.SearchHistoryService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final WardRepository wardRepository;
    private final FurnishingRepository furnishingRepository;
    private final PropertyMapper propertyMapper;
    private final EntityManager em;
    private final UserManagementLogRepository userManagementLogRepository;
    private final AuthService authService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final ObjectMapper om = new ObjectMapper();
    private final SearchHistoryService searchHistoryService;

    private void logAction(User admin, User target, String action){
        UserManagementLog log = UserManagementLog.builder()
                .admin(admin)
                .targetUser(target)
                .action(action)
                .createdAt(LocalDateTime.now())
                .build();
        userManagementLogRepository.save(log);
    }

    /* =================== CREATE =================== */
    @Transactional
    @Override
    public Property create(PropertyDto dto) {
        if (dto == null) throw new IllegalArgumentException("Request is null");
        if (dto.getPropertyType() == null) throw new IllegalArgumentException("propertyType is required");
        if (dto.getLandlord() == null || dto.getLandlord().getUserId() == null)
            throw new IllegalArgumentException("landlord.userId is required");
        if (dto.getAddress() == null) throw new IllegalArgumentException("Address is required");

        // Load landlord
        User landlord = userRepository.findById(dto.getLandlord().getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Landlord not found: " + dto.getLandlord().getUserId()));

        // Load ward (nếu có wardId)
        Ward ward = null;
        if (dto.getAddress().getWardId() != null) {
            ward = wardRepository.findById(dto.getAddress().getWardId())
                    .orElseThrow(() -> new EntityNotFoundException("Ward not found: " + dto.getAddress().getWardId()));
        }

        // Chuyển AddressDto -> Address entity
        Address address = propertyMapper.getAddressMapper().toEntity(dto.getAddress(), ward);

        // Property entity
        Property property = propertyMapper.toEntity(dto, address);
        property.setLandlord(landlord);
        property.setPostStatus(PostStatus.PENDING);
        property.setRejectedReason(null);
        property.setPublishedAt(null);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        // Furnishings
        List<PropertyFurnishing> fixed = new ArrayList<>();
        if (dto.getFurnishings() != null) {
            for (PropertyFurnishingDto fDto : dto.getFurnishings()) {
                if (fDto.getFurnishingId() == null || fDto.getFurnishingId().isBlank())
                    throw new IllegalArgumentException("Each furnishing must include furnishingId");

                Furnishings furnishing = furnishingRepository.findById(fDto.getFurnishingId())
                        .orElseThrow(() -> new EntityNotFoundException("Furnishing not found: " + fDto.getFurnishingId()));

                PropertyFurnishing pf = new PropertyFurnishing();
                pf.setProperty(property);
                pf.setFurnishing(furnishing);
                pf.setQuantity(fDto.getQuantity() != null ? fDto.getQuantity() : 1);
                fixed.add(pf);
            }
        }
        property.setFurnishings(fixed);

        Property saved = propertyRepository.save(property);
        realtimeNotificationService.notifyAdminsPropertyCreated(propertyMapper.toDto(saved));
        return saved;
    }

    /* =================== GET =================== */
    @Transactional(readOnly = true)
    @Override
    public Optional<PropertyDto> getById(String id) {
        return propertyRepository.findById(id)
                .map(propertyMapper::toDto);
    }

    /* =================== LIST =================== */
    @Transactional(readOnly = true)
    @Override
    public Page<PropertyDto> list(String landlordId, String postStatus, String type, String keyword, Pageable pageable) {
        Specification<Property> spec = PropertySpecs.landlordIdEq(landlordId)
                .and(PropertySpecs.postStatusEq(postStatus))
                .and(PropertySpecs.typeEq(type))
                .and(PropertySpecs.keywordLike(keyword));

        Page<Property> page = propertyRepository.findAll(spec, pageable);
        return page.map(propertyMapper::toDto);
    }

    /* =================== UPDATE (→ PENDING) =================== */
    @Transactional
    @Override
    public Property update(String id, PropertyDto dto) {
        Property existing = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));

        // Cập nhật các field cơ bản
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getArea() != null) existing.setArea(dto.getArea());
        if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
        if (dto.getDeposit() != null) existing.setDeposit(dto.getDeposit());
        if (dto.getBuildingName() != null) existing.setBuildingName(dto.getBuildingName());
        if (dto.getApartmentCategory() != null) existing.setApartmentCategory(dto.getApartmentCategory());
        if (dto.getBedrooms() != null) existing.setBedrooms(dto.getBedrooms());
        if (dto.getBathrooms() != null) existing.setBathrooms(dto.getBathrooms());
        if (dto.getRoomNumber() != null) existing.setRoomNumber(dto.getRoomNumber());
        if (dto.getFloorNo() != null) existing.setFloorNo(dto.getFloorNo());

        // Update Address
        if (dto.getAddress() != null) {
            Ward ward = null;
            if (dto.getAddress().getWardId() != null) {
                ward = wardRepository.findById(dto.getAddress().getWardId())
                        .orElseThrow(() -> new EntityNotFoundException("Ward not found: " + dto.getAddress().getWardId()));
            }

            Address addr = existing.getAddress();
            if (addr == null) {
                addr = propertyMapper.getAddressMapper().toEntity(dto.getAddress(), ward);
            } else {
                propertyMapper.getAddressMapper().updateEntity(addr, dto.getAddress(), ward);
            }
            existing.setAddress(addr);
        }

        existing.setPostStatus(PostStatus.PENDING);
        existing.setRejectedReason(null);
        existing.setPublishedAt(null);
        existing.setUpdatedAt(LocalDateTime.now());

        Property saved = propertyRepository.save(existing);
        realtimeNotificationService.notifyAdminsPropertyUpdated(propertyMapper.toDto(saved));
        return saved;
    }

    /* =================== CHANGE STATUS =================== */
    @Transactional
    @Override
    public void changeStatus(String id, PostStatus status, String rejectedReason) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));

        p.setPostStatus(status);
        p.setUpdatedAt(LocalDateTime.now());

        if (status == PostStatus.APPROVED) {
            p.setRejectedReason(null);
            p.setPublishedAt(LocalDateTime.now());
        } else if (status == PostStatus.REJECTED) {
            p.setRejectedReason(rejectedReason != null ? rejectedReason : "Rejected");
            p.setPublishedAt(null);
        } else { // PENDING / INACTIVE ...
            p.setRejectedReason(null);
            if (status != PostStatus.APPROVED) p.setPublishedAt(null);
        }
        // Log action
        User admin = authService.getCurrentUser();
        logAction(admin, p.getLandlord(), "CHANGE_PROPERTY_STATUS: " + status + (rejectedReason != null ? " REASON: " + rejectedReason : ""));
        propertyRepository.save(p);

        PropertyDto dto = propertyMapper.toDto(p);

        realtimeNotificationService.notifyAdminsPropertyStatusChanged(dto, status, rejectedReason);
    }

    /* =================== DELETE =================== */
    @Transactional
    @Override
    public void delete(String id) {
        if (!propertyRepository.existsById(id)) {
            throw new EntityNotFoundException("Property not found: " + id);
        }
        propertyRepository.deleteById(id);
        // Nếu muốn xoá media S3: inject mediaRepo/mediaService và xoá trước
    }

    /* =================== SEARCH =================== */
    @Override
    public Page<PropertyDto> search(String userId,
                                    String keyword,
                                    Integer priceMin, Integer priceMax,
                                    Integer areaMin, Integer areaMax,
                                    String apartmentCategory,
                                    Integer floorNo,
                                    String roomNumber,
                                    Integer bathrooms, Integer bedrooms,
                                    Integer capacity, Integer parkingSlots,
                                    String buildingName, String propertyType,
                                    int page, int size) {

        Specification<Property> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        // nếu có field address.fullAddress: cb.like(cb.lower(root.get("address").get("fullAddress")), like)
                        cb.like(cb.lower(root.get("buildingName")), like)
                ));
            }
            if (priceMin != null) ps.add(cb.ge(root.get("price"), priceMin));
            if (priceMax != null) ps.add(cb.le(root.get("price"), priceMax));

            if (areaMin != null) ps.add(cb.ge(root.get("area"), areaMin));
            if (areaMax != null) ps.add(cb.le(root.get("area"), areaMax));

            if (apartmentCategory != null && !apartmentCategory.isBlank())
                ps.add(cb.equal(root.get("apartmentCategory"), apartmentCategory)); // Enum/String tuỳ entity

            if (floorNo != null) ps.add(cb.equal(root.get("floorNo"), floorNo));
            if (roomNumber != null && !roomNumber.isBlank())
                ps.add(cb.equal(cb.lower(root.get("roomNumber")), roomNumber.toLowerCase()));

            if (bathrooms != null) ps.add(cb.equal(root.get("bathrooms"), bathrooms));
            if (bedrooms  != null) ps.add(cb.equal(root.get("bedrooms"), bedrooms));
            if (capacity  != null) ps.add(cb.ge(root.get("capacity"), capacity));
            if (parkingSlots != null) ps.add(cb.ge(root.get("parkingSlots"), parkingSlots));

            if (buildingName != null && !buildingName.isBlank())
                ps.add(cb.like(cb.lower(root.get("buildingName")), "%" + buildingName.toLowerCase() + "%"));

            if (propertyType != null && !propertyType.isBlank())
                ps.add(cb.equal(root.get("propertyType"), propertyType)); // Enum/String tuỳ entity

            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> pageData = propertyRepository.findAll(spec, pageable);
        Page<PropertyDto> result = pageData.map(propertyMapper::toDto);

        // LƯU LỊCH SỬ
        if (userId != null) {
            ObjectNode filters = om.createObjectNode();
            put(filters, "priceMin", priceMin);
            put(filters, "priceMax", priceMax);
            put(filters, "areaMin",  areaMin);
            put(filters, "areaMax",  areaMax);
            put(filters, "apartmentCategory", apartmentCategory);
            put(filters, "floorNo",  floorNo);
            put(filters, "roomNumber", roomNumber);
            put(filters, "bathrooms", bathrooms);
            put(filters, "bedrooms",  bedrooms);
            put(filters, "capacity",  capacity);
            put(filters, "parkingSlots", parkingSlots);
            put(filters, "buildingName", buildingName);
            put(filters, "propertyType", propertyType);

            searchHistoryService.saveHistory(userId, keyword, filters);
        }

        return result;
    }

    private static void put(ObjectNode n, String k, Integer v) { if (v != null) n.put(k, v); }
    private static void put(ObjectNode n, String k, String v)  { if (v != null && !v.isBlank()) n.put(k, v); }

    /* ===== Specs cho list() ===== */
    static class PropertySpecs {
        static Specification<Property> landlordIdEq(String landlordId) {
            return (root, cq, cb) -> {
                if (landlordId == null || landlordId.isBlank()) return cb.conjunction();
                return cb.equal(root.get("landlord").get("userId"), landlordId);
            };
        }
        static Specification<Property> postStatusEq(String postStatus) {
            return (root, cq, cb) -> {
                if (postStatus == null || postStatus.isBlank()) return cb.conjunction();
                try {
                    PostStatus st = PostStatus.valueOf(postStatus.toUpperCase(Locale.ROOT));
                    return cb.equal(root.get("postStatus"), st);
                } catch (Exception e) {
                    return cb.disjunction();
                }
            };
        }
        static Specification<Property> typeEq(String type) {
            return (root, cq, cb) -> {
                if (type == null || type.isBlank()) return cb.conjunction();
                try {
                    PropertyType t = PropertyType.valueOf(type.toUpperCase(Locale.ROOT));
                    return cb.equal(root.get("propertyType"), t);
                } catch (Exception e) {
                    return cb.disjunction();
                }
            };
        }
        static Specification<Property> keywordLike(String keyword) {
            return (root, cq, cb) -> {
                if (keyword == null || keyword.isBlank()) return cb.conjunction();
                String like = "%" + keyword.toLowerCase(Locale.ROOT).trim() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("buildingName")), like),
                        cb.like(cb.lower(root.get("roomNumber")), like)
                );
            };
        }
    }
}
