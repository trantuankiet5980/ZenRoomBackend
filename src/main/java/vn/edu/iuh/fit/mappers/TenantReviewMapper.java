package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.TenantReviewDto;
import vn.edu.iuh.fit.entities.TenantReview;

@Component
@RequiredArgsConstructor
public class TenantReviewMapper {
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;

    /** Entity -> DTO */
    public TenantReviewDto toDto(TenantReview e) {
        if (e == null) return null;
        return new TenantReviewDto(
                e.getTenantReviewId(),
                bookingMapper.toDto(e.getBooking()),
                userMapper.toDto(e.getLandlord()),
                userMapper.toDto(e.getTenant()),
                e.getRating(),
                e.getComment(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** DTO -> Entity */
    public TenantReview toEntity(TenantReviewDto d) {
        if (d == null) return null;
        return TenantReview.builder()
                .tenantReviewId(d.getTenantReviewId())
                .rating(d.getRating())
                .comment(d.getComment())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
