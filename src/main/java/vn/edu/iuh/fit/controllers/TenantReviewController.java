package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.TenantReviewDto;
import vn.edu.iuh.fit.services.TenantReviewService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/tenant-reviews")
@RequiredArgsConstructor
public class TenantReviewController {
    private final TenantReviewService service;

    /** Landlord create/update tenant review (DTO.tenantReviewId null=create; !=null=update 24h) */
    @PostMapping
    public TenantReviewDto upsert(@RequestBody TenantReviewDto dto, Principal principal) {
        return service.upsert(principal.getName(), dto);
    }

    /** Landlord delete trong 24h */
    @DeleteMapping("/{tenantReviewId}")
    public void delete(@PathVariable String tenantReviewId, Principal principal) {
        service.delete(principal.getName(), tenantReviewId);
    }

    /** Xem đánh giá theo booking (tenant/landlord của booking được xem) */
    @GetMapping("/by-booking/{bookingId}")
    public TenantReviewDto getByBooking(@PathVariable String bookingId, Principal principal) {
        return service.getByBooking(bookingId, principal.getName());
    }

    /** Danh sách đánh giá về 1 tenant (phân trang) */
    @GetMapping("/about-tenant/{tenantId}")
    public Page<TenantReviewDto> listAboutTenant(@PathVariable String tenantId,
                                                 @RequestParam(defaultValue="0") int page,
                                                 @RequestParam(defaultValue="10") int size,
                                                 @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return service.listAboutTenant(tenantId, pageable);
    }

    /** Danh sách đánh giá do landlord đã viết (phân trang) */
    @GetMapping("/me")
    public Page<TenantReviewDto> myReviews(Principal principal,
                                           @RequestParam(defaultValue="0") int page,
                                           @RequestParam(defaultValue="10") int size,
                                           @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return service.listByLandlord(principal.getName(), pageable);
    }

    /** Các booking COMPLETED nhưng chưa đánh giá (nhắc landlord chấm) */
    @GetMapping("/pending-to-rate")
    public Page<String> pendingToRate(Principal principal,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return service.listPendingBookingsToRate(principal.getName(), pageable);
    }

    private Sort parseSort(String sort) {
        String[] p = sort.split(",");
        if (p.length == 2) return Sort.by("DESC".equalsIgnoreCase(p[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, p[0]);
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
