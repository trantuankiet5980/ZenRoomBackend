package vn.edu.iuh.fit.controllers;

import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.DiscountCodeDto;
import vn.edu.iuh.fit.services.DiscountCodeService;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/discount-codes")
@RequiredArgsConstructor
public class DiscountCodeController {

    private final DiscountCodeService service;

    // ADMIN tạo mã
    @PostMapping
    public DiscountCodeDto create(@RequestBody DiscountCodeDto dto, Principal principal) {
        return service.create(principal.getName(), dto);
    }

    // ADMIN cập nhật mã
    @PutMapping
    public DiscountCodeDto update(@RequestBody DiscountCodeDto dto, Principal principal) {
        return service.update(principal.getName(), dto);
    }

    // ADMIN xoá mã (tuỳ)
    @DeleteMapping("/{codeId}")
    public void delete(@PathVariable String codeId, Principal principal) {
        service.delete(principal.getName(), codeId);
    }

    // Xem chi tiết
    @GetMapping("/{codeId}")
    public DiscountCodeDto get(@PathVariable String codeId) {
        return service.get(codeId);
    }

    // Danh sách + search
    @GetMapping
    public Page<DiscountCodeDto> list(@RequestParam(required=false) String q,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size,
                                      @RequestParam(defaultValue="validFrom,DESC") String sort) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return service.list(q, pageable);
    }

    // Preview (không trừ lượt)
    @GetMapping("/preview")
    public java.util.Map<String, Object> preview(@RequestParam String code,
                                                 @RequestParam BigDecimal subtotal) {
        var off = service.previewDiscount(code, subtotal);
        return java.util.Map.of("code", code.trim(), "subtotal", subtotal, "discount", off, "payable", subtotal.subtract(off));
    }

    // Apply (trừ lượt) quyeefn tenant
    @PostMapping("/apply")
    public java.util.Map<String, Object> apply(@RequestParam String code,
                                               @RequestParam BigDecimal subtotal) {
        var off = service.applyDiscount(code, subtotal);
        return java.util.Map.of("code", code.trim(), "subtotal", subtotal, "discount", off, "payable", subtotal.subtract(off));
    }

    private Sort parseSort(String sort) {
        String[] p = sort.split(",");
        if (p.length == 2) return Sort.by("DESC".equalsIgnoreCase(p[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, p[0]);
        return Sort.by(Sort.Direction.DESC, "validFrom");
    }

}
