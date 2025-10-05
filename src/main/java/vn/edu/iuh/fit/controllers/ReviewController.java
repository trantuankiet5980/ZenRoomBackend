package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.ReviewDto;
import vn.edu.iuh.fit.dtos.ReviewReplyDto;
import vn.edu.iuh.fit.dtos.ReviewStatsDto;
import vn.edu.iuh.fit.services.ReviewService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    /** Tenant create/update review (DTO.reviewId null=create; !=null=update 24h) */
    @PostMapping
    public ReviewDto upsert(@RequestBody ReviewDto dto, Principal principal) {
        return reviewService.upsertTenantReview(principal.getName(), dto);
    }

    /** Tenant delete review trong 24h */
    @DeleteMapping("/{reviewId}")
    public void delete(@PathVariable String reviewId, Principal principal) {
        reviewService.deleteTenantReview(principal.getName(), reviewId);
    }

    /** Landlord create/update reply (DTO.replyId null=create; !=null=update 24h) */
    @PostMapping("/reply")
    public ReviewReplyDto upsertReply(@RequestBody ReviewReplyDto dto, Principal principal) {
        return reviewService.upsertLandlordReply(principal.getName(), dto);
    }

    /** Xem review theo booking (tenant/landlord của booking mới xem) */
    @GetMapping("/by-booking/{bookingId}")
    public ReviewDto getByBooking(@PathVariable String bookingId, Principal principal) {
        return reviewService.getByBooking(bookingId, principal.getName());
    }

    /** Danh sách review theo property (phân trang) */
    @GetMapping("/by-property/{propertyId}")
    public Page<ReviewDto> listByProperty(@PathVariable String propertyId,
                                          @RequestParam(defaultValue="0") int page,
                                          @RequestParam(defaultValue="10") int size,
                                          @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return reviewService.listByProperty(propertyId, pageable);
    }

    /** Danh sách review của một tenant (phân trang) */
    @GetMapping("/by-tenant/{tenantId}")
    public Page<ReviewDto> listByTenant(@PathVariable String tenantId,
                                        @RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="10") int size,
                                        @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return reviewService.listByTenant(tenantId, pageable);
    }

    /** Thống kê tổng số và trung bình rating của landlord */
    @GetMapping("/landlord/{landlordId}/stats")
    public ReviewStatsDto landlordStats(@PathVariable String landlordId) {
        return reviewService.getLandlordReviewStats(landlordId);
    }

    private Sort parseSort(String sort) {
        String[] p = sort.split(",");
        if (p.length == 2) return Sort.by("DESC".equalsIgnoreCase(p[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, p[0]);
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
