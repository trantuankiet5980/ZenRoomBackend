package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
//import vn.edu.iuh.fit.dtos.PropertyCreateDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.mappers.PropertyMapper;
import vn.edu.iuh.fit.services.PropertyService;

import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyMapper propertyMapper;

    /** Đăng bài (BUILDING/ROOM). ROOM cần ?parentId=... */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PropertyDto dto) {
        Property saved = propertyService.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/properties/" + saved.getPropertyId()))
                .body(propertyMapper.toDto(saved));
    }

    /** Lấy bài theo id */
    @GetMapping("{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return propertyService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Danh sách có phân trang + lọc */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) String postStatus,   // PENDING|APPROVE|REJECTED
            @RequestParam(required = false) String type,         // BUILDING|ROOM
            @RequestParam(required = false, name = "q") String keyword
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(size, 100), s);

        Page<PropertyDto> dtoPage = propertyService.list(landlordId, postStatus, type, keyword, pageable);

        return ResponseEntity.ok(Map.of(
                "totalElements", dtoPage.getTotalElements(),
                "totalPages", dtoPage.getTotalPages(),
                "page", dtoPage.getNumber(),
                "size", dtoPage.getSize(),
                "sort", sort,
                "content", dtoPage.getContent()

        ));
    }

    @GetMapping("/search")
    public Page<PropertyDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer priceMin,
            @RequestParam(required = false) Integer priceMax,
            @RequestParam(required = false) Integer areaMin,
            @RequestParam(required = false) Integer areaMax,
            @RequestParam(required = false) String apartmentCategory,
            @RequestParam(required = false) Integer floorNo,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) Integer parkingSlots,
            @RequestParam(required = false) String buildingName,
            @RequestParam(required = false) String propertyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Principal principal
    ) {
        String userId = (principal != null) ? principal.getName() : null;
        return propertyService.search(
                userId, keyword,
                priceMin, priceMax,
                areaMin, areaMax,
                apartmentCategory,
                floorNo, roomNumber,
                bathrooms, bedrooms,
                capacity, parkingSlots,
                buildingName, propertyType,
                page, size
        );
    }

    /** Duyệt bài / đổi trạng thái (APPROVE/REJECTED//PENDING) */
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<Void> changeStatus(
            @PathVariable String id,
            @RequestParam PostStatus status,
            @RequestParam(required = false) String reason) {

        propertyService.changeStatus(id, status, reason);
        return ResponseEntity.noContent().build();
    }

    /** Cập nhật bài đăng (đưa về PENDING) */
    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody PropertyDto dto) {
        Property updated = propertyService.update(id, dto);
        return ResponseEntity.ok(propertyMapper.toDto(updated));
    }

    /** Xoá bài */
    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "ASC".equalsIgnoreCase(parts[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(dir, prop);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    /** Wrapper phân trang */
    public record PageResponse<T>(
            int page, int size, long totalElements, int totalPages, boolean first, boolean last, Object content
    ) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                    p.isFirst(), p.isLast(), p.getContent()
            );
        }
    }

}
