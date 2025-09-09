package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.FurnishingsDto;
import vn.edu.iuh.fit.services.FurnishingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/furnishings")
public class FurnishingsController {

    private final FurnishingService service;

    // List phân trang + search
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "furnishingName,ASC") String sort,
            @RequestParam(required = false, name = "q") String keyword
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100), s);
        return ResponseEntity.ok(service.list(keyword, pageable));
        // Nếu bạn có PageResponse.of(page): return ResponseEntity.ok(PageResponse.of(service.list(...)));
    }

    // Lấy 1 item theo id
    @GetMapping("{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return service.get(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tạo mới
    @PostMapping
    public ResponseEntity<?> create(@RequestBody FurnishingsDto dto) {
        FurnishingsDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // Cập nhật tên
    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody FurnishingsDto dto) {
        FurnishingsDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // Xoá
    @DeleteMapping("{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        // ví dụ: "furnishingName,ASC" hoặc "createdAt,DESC"
        String[] parts = sort.split(",");
        String prop = parts.length > 0 ? parts[0].trim() : "furnishingName";
        Sort.Direction dir = (parts.length > 1 && "DESC".equalsIgnoreCase(parts[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, prop);
    }
}
