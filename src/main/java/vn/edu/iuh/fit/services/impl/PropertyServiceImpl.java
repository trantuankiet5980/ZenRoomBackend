package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FurnishingRepository furnishingRepository;
    private final PropertyMapper propertyMapper;
    private final EntityManager em;
    private final UserManagementLogRepository userManagementLogRepository;
    private final AuthService authService;
    private final RealtimeNotificationService realtimeNotificationService;
//    private final SimpMessagingTemplate messaging;

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
        if (dto.getAddress() == null)
            throw new IllegalArgumentException("Address is required");

        // Map cơ bản từ DTO sang Entity (nhờ mapper của bạn)
        Property entity = propertyMapper.toEntity(dto);

        // Đảm bảo landlord & address là managed entity (tránh detached/transient)
        User managedLandlord = userRepository.findById(dto.getLandlord().getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Landlord not found: " + dto.getLandlord().getUserId()));
        entity.setLandlord(managedLandlord);

        // Address: nếu AddressDto có id thì dùng lại; nếu không, để cascade ALL của bạn persist
        if (dto.getAddress().getAddressId() != null) {
            Address managedAddress = addressRepository.findById(dto.getAddress().getAddressId())
                    .orElseThrow(() -> new EntityNotFoundException("Address not found: " + dto.getAddress().getAddressId()));
            entity.setAddress(managedAddress);
        }

        // Trạng thái bài viết: luôn PENDING khi tạo mới
        entity.setPostStatus(PostStatus.PENDING);
        entity.setRejectedReason(null);
        entity.setPublishedAt(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        // ===== Build furnishings từ danh mục có sẵn =====
        List<PropertyFurnishing> fixed = new ArrayList<>();
        if (dto.getFurnishings() != null) {
            for (PropertyFurnishingDto fDto : dto.getFurnishings()) {
                if (fDto.getFurnishingId() == null || fDto.getFurnishingId().isBlank()) {
                    throw new IllegalArgumentException("Each furnishing must include furnishingId");
                }
                // Thay vì getReference (nổ khi id không tồn tại lúc flush), bạn có thể findById để báo lỗi sớm:
                Furnishings furnishing = furnishingRepository.findById(fDto.getFurnishingId())
                        .orElseThrow(() -> new EntityNotFoundException("Furnishing not found: " + fDto.getFurnishingId()));

                PropertyFurnishing pf = new PropertyFurnishing();
                pf.setProperty(entity);
                pf.setFurnishing(furnishing);
                pf.setQuantity(fDto.getQuantity() != null ? fDto.getQuantity() : 1);

                fixed.add(pf);
            }
        }
        entity.setFurnishings(fixed);

        Property saved = propertyRepository.save(entity);

        // Gửi realtime + lưu DB
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

        // Cập nhật các field được phép (không đổi landlord ở đây)
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

        // Address: nếu DTO có id → map sang managed entity; nếu không, có thể merge AddressDto (tuỳ bạn)
        if (dto.getAddress() != null) {
            if (dto.getAddress().getAddressId() != null) {
                Address managedAddress = addressRepository.findById(dto.getAddress().getAddressId())
                        .orElseThrow(() -> new EntityNotFoundException("Address not found: " + dto.getAddress().getAddressId()));
                existing.setAddress(managedAddress);
            } else {
                // merge từng field nếu bạn muốn cập nhật địa chỉ hiện hữu:
                Address addr = existing.getAddress();
                if (addr == null) {
                    addr = propertyMapper.toEntity(dto).getAddress(); // lấy object mới từ mapper
                } else {
                    // copy field đơn giản (tuỳ DTO AddressDto của bạn)
                    var a = dto.getAddress();
                    if (a.getProvince() != null) addr.setProvince(a.getProvince());
                    if (a.getDistrict() != null) addr.setDistrict(a.getDistrict());
                    if (a.getWard() != null) addr.setWard(a.getWard());
                    if (a.getStreet() != null) addr.setStreet(a.getStreet());
                    if (a.getHouseNumber() != null) addr.setHouseNumber(a.getHouseNumber());
                    if (a.getAddressFull() != null) addr.setAddressFull(a.getAddressFull());
                    if (a.getLatitude() != null) addr.setLatitude(a.getLatitude());
                    if (a.getLongitude() != null) addr.setLongitude(a.getLongitude());
                }
                existing.setAddress(addr);
            }
        }

        // Sau khi chỉnh, đưa về PENDING để duyệt lại
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
