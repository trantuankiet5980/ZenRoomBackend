package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.ReviewDto;
import vn.edu.iuh.fit.dtos.ReviewReplyDto;
import vn.edu.iuh.fit.dtos.ReviewStatsDto;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Review;
import vn.edu.iuh.fit.entities.ReviewReply;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.mappers.ReviewMapper;
import vn.edu.iuh.fit.mappers.ReviewReplyMapper;
import vn.edu.iuh.fit.repositories.*;
import vn.edu.iuh.fit.services.ReviewService;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepo;
    private final ReviewReplyRepository replyRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper replyMapper;

    @Transactional
    @Override
    public ReviewDto upsertTenantReview(String tenantId, ReviewDto dto) {
        if (dto.getReviewId() == null) {
            // CREATE
            if (dto.getBooking() == null || dto.getBooking().getBookingId() == null)
                throw new IllegalArgumentException("booking (bookingId) is required");
            if (dto.getRating() < 1 || dto.getRating() > 5)
                throw new IllegalArgumentException("rating must be 1..5");

            Booking b = bookingRepo.findById(dto.getBooking().getBookingId()).orElseThrow();
            if (!b.getTenant().getUserId().equals(tenantId)) throw new SecurityException("Not your booking");
            if (b.getBookingStatus() != BookingStatus.COMPLETED)
                throw new IllegalStateException("Chỉ có thể đánh giá sau khi hoàn thành");
            LocalDateTime completedAt = b.getUpdatedAt();
            LocalDateTime now = LocalDateTime.now();
            if (completedAt == null || now.isAfter(completedAt.plusDays(10)))
                throw new IllegalStateException("Chỉ được phép đánh giá trong vòng 10 ngày sau khi hoàn thành");
            if (reviewRepo.existsByBooking_BookingIdAndTenant_UserId(b.getBookingId(), tenantId))
                throw new IllegalStateException("Bạn đã đánh giá booking này");

            User tenant = userRepo.findById(tenantId).orElseThrow();

            Review r = new Review();
            r.setReviewId(java.util.UUID.randomUUID().toString());
            r.setBooking(b);
            r.setTenant(tenant);
            r.setRating(dto.getRating());
            r.setComment(dto.getComment());
            r.setCreatedAt(now);
            r.setUpdatedAt(null);

            return reviewMapper.toDto(reviewRepo.save(r));
        } else {
            // UPDATE (24h)
            Review r = reviewRepo.findById(dto.getReviewId()).orElseThrow();
            if (!r.getTenant().getUserId().equals(tenantId)) throw new SecurityException("Not your review");
            if (r.getCreatedAt() == null || Duration.between(r.getCreatedAt(), LocalDateTime.now()).toHours() > 24)
                throw new IllegalStateException("Chỉ được phép chỉnh sửa đánh giá trong vòng 24 giờ");

            if (dto.getRating() >= 1 && dto.getRating() <= 5) r.setRating(dto.getRating());
            r.setComment(dto.getComment());
            r.setUpdatedAt(LocalDateTime.now());
            return reviewMapper.toDto(reviewRepo.save(r));
        }
    }

    @Transactional
    @Override
    public void deleteTenantReview(String tenantId, String reviewId) {
        Review r = reviewRepo.findById(reviewId).orElseThrow();
        if (!r.getTenant().getUserId().equals(tenantId)) throw new SecurityException("Not your review");
        if (r.getCreatedAt() == null || Duration.between(r.getCreatedAt(), LocalDateTime.now()).toHours() > 24)
            throw new IllegalStateException("Chỉ được phép xóa đánh giá trong vòng 24 giờ");
        // xóa reply nếu có (hoặc dùng cascade)
        replyRepo.findByReview_ReviewId(reviewId).ifPresent(replyRepo::delete);
        reviewRepo.delete(r);
    }

    @Transactional
    @Override
    public ReviewReplyDto upsertLandlordReply(String landlordId, ReviewReplyDto dto) {
        if (dto.getReplyId() == null) {
            // CREATE
            if (dto.getReviewId() == null)
                throw new IllegalArgumentException("reviewId is required");

            Review r = reviewRepo.findById(dto.getReviewId()).orElseThrow();
            User landlord = r.getBooking().getProperty().getLandlord();
            if (!landlord.getUserId().equals(landlordId))
                throw new SecurityException("Chỉ có chủ nhà mới có thể trả lời đánh giá");
            if (replyRepo.existsByReview_ReviewId(r.getReviewId()))
                throw new IllegalStateException("Reply already exists");
            LocalDateTime reviewCreatedAt = r.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();
            if (reviewCreatedAt == null || now.isAfter(reviewCreatedAt.plusDays(10)))
                throw new IllegalStateException("Chỉ được phép trả lời trong vòng 10 ngày kể từ khi nguời thuê đánh giá");
            ReviewReply rp = new ReviewReply();
            rp.setReplyId(java.util.UUID.randomUUID().toString());
            rp.setReview(r);
            rp.setLandlord(landlord);
            rp.setReplyText(dto.getReplyText());
            rp.setCreatedAt(LocalDateTime.now());
            rp.setUpdatedAt(null);
            ReviewReply saved = replyRepo.save(rp);
            r.setReply(saved); // set reply cho review

            return replyMapper.toDto(saved);
        } else {
            // UPDATE (24h)
            ReviewReply rp = replyRepo.findById(dto.getReplyId()).orElseThrow();
            if (!rp.getLandlord().getUserId().equals(landlordId)) throw new SecurityException("Not your reply");
            if (rp.getCreatedAt() == null || Duration.between(rp.getCreatedAt(), LocalDateTime.now()).toHours() > 24)
                throw new IllegalStateException("Editable within 24h");

            rp.setReplyText(dto.getReplyText());
            rp.setUpdatedAt(LocalDateTime.now());
            ReviewReply updated = replyRepo.save(rp);
            if (updated.getReview() != null) {
                updated.getReview().setReply(updated);
            }
            return replyMapper.toDto(updated);
        }
    }

    @Override
    public ReviewDto getByBooking(String bookingId, String requesterId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        boolean isTenant = b.getTenant() != null && b.getTenant().getUserId().equals(requesterId);
        boolean isLandlord = b.getProperty() != null && b.getProperty().getLandlord() != null
                && b.getProperty().getLandlord().getUserId().equals(requesterId);
        if (!isTenant && !isLandlord) throw new SecurityException("Not allowed");

        return reviewRepo.findByBooking_BookingId(bookingId).map(reviewMapper::toDto).orElse(null);
    }

    @Override
    public Page<ReviewDto> listByProperty(String propertyId, Pageable pageable) {
        return reviewRepo.findByPropertyId(propertyId, pageable).map(reviewMapper::toDto);
    }

    @Override
    public Page<ReviewDto> listByTenant(String tenantId, Pageable pageable) {
        return reviewRepo.findByTenant_UserIdOrderByCreatedAtDesc(tenantId, pageable).map(reviewMapper::toDto);
    }

    @Override
    public ReviewStatsDto getLandlordReviewStats(String landlordId) {
        LandlordReviewStatsProjection stats = reviewRepo.findLandlordReviewStats(landlordId);
        long totalReviews = stats != null && stats.getTotalReviews() != null ? stats.getTotalReviews() : 0L;
        double averageRating = stats != null && stats.getAverageRating() != null ? stats.getAverageRating() : 0d;
        return new ReviewStatsDto(totalReviews, averageRating);
    }
}
