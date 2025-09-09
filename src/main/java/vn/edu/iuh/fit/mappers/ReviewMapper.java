package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ReviewDto;
import vn.edu.iuh.fit.entities.Review;

@Component
public class ReviewMapper {
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;

    public ReviewMapper(BookingMapper bookingMapper, UserMapper userMapper) {
        this.bookingMapper = bookingMapper;
        this.userMapper = userMapper;
    }

    /** Entity -> DTO */
    public ReviewDto toDto(Review e) {
        if (e == null) return null;
        return new ReviewDto(
                e.getReviewId(),
                bookingMapper.toDto(e.getBooking()),
                userMapper.toDto(e.getTenant()),
                e.getRating(),
                e.getComment(),
                e.getCreatedAt()
        );
    }

    /** DTO -> Entity (tenant & booking gán trong service qua getReference để tránh vòng lặp/lazy) */
    public Review toEntity(ReviewDto d) {
        if (d == null) return null;
        return Review.builder()
                .reviewId(d.getReviewId())
                .rating(d.getRating())
                .comment(d.getComment())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
