package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.TenantReviewDto;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.TenantReview;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.mappers.TenantReviewMapper;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.TenantReviewRepository;
import vn.edu.iuh.fit.services.TenantReviewService;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TenantReviewServiceImpl implements TenantReviewService {
    private final TenantReviewRepository trRepo;
    private final BookingRepository bookingRepo;
    private final TenantReviewMapper mapper;

    @Override
    public TenantReviewDto upsert(String landlordId, TenantReviewDto dto) {
        if (dto.getTenantReviewId() == null) {
            // CREATE
            if (dto.getBooking() == null || dto.getBooking().getBookingId() == null)
                throw new IllegalArgumentException("booking (bookingId) is required");
            if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5)
                throw new IllegalArgumentException("rating must be 1..5");

            Booking b = bookingRepo.findById(dto.getBooking().getBookingId()).orElseThrow();
            if (b.getProperty() == null || b.getProperty().getLandlord() == null
                    || !b.getProperty().getLandlord().getUserId().equals(landlordId))
                throw new SecurityException("Only landlord of this booking can rate tenant");
            if (b.getBookingStatus() != BookingStatus.COMPLETED)
                throw new IllegalStateException("Can rate only after COMPLETED");
            if (trRepo.existsByBooking_BookingId(b.getBookingId()))
                throw new IllegalStateException("Already rated for this booking");

            TenantReview tr = new TenantReview();
            tr.setTenantReviewId(java.util.UUID.randomUUID().toString());
            tr.setBooking(b);
            tr.setLandlord(b.getProperty().getLandlord());
            tr.setTenant(b.getTenant());
            tr.setRating(dto.getRating());
            tr.setComment(dto.getComment());
            tr.setCreatedAt(LocalDateTime.now());
            tr.setUpdatedAt(null);

            return mapper.toDto(trRepo.save(tr));
        } else {
            // UPDATE (24h)
            TenantReview tr = trRepo.findById(dto.getTenantReviewId()).orElseThrow();
            if (!tr.getLandlord().getUserId().equals(landlordId)) throw new SecurityException("Not your review");
            if (tr.getCreatedAt() == null || Duration.between(tr.getCreatedAt(), LocalDateTime.now()).toHours() > 24)
                throw new IllegalStateException("Editable within 24h");
            if (dto.getRating() != null) {
                if (dto.getRating() < 1 || dto.getRating() > 5) throw new IllegalArgumentException("rating 1..5");
                tr.setRating(dto.getRating());
            }
            tr.setComment(dto.getComment());
            tr.setUpdatedAt(LocalDateTime.now());
            return mapper.toDto(trRepo.save(tr));
        }
    }

    @Transactional
    @Override
    public void delete(String landlordId, String tenantReviewId) {
        TenantReview tr = trRepo.findById(tenantReviewId).orElseThrow();
        if (!tr.getLandlord().getUserId().equals(landlordId)) throw new SecurityException("Not your review");
        if (tr.getCreatedAt() == null || Duration.between(tr.getCreatedAt(), LocalDateTime.now()).toHours() > 24)
            throw new IllegalStateException("Deletable within 24h");
        trRepo.delete(tr);
    }

    @Override
    public TenantReviewDto getByBooking(String bookingId, String requesterId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        boolean isTenant = b.getTenant() != null && b.getTenant().getUserId().equals(requesterId);
        boolean isLandlord = b.getProperty() != null && b.getProperty().getLandlord() != null
                && b.getProperty().getLandlord().getUserId().equals(requesterId);
        if (!isTenant && !isLandlord) throw new SecurityException("Not allowed");
        return trRepo.findByBooking_BookingId(bookingId).map(mapper::toDto).orElse(null);
    }

    @Override
    public Page<TenantReviewDto> listAboutTenant(String tenantId, Pageable pageable) {
        return trRepo.findByTenant_UserIdOrderByCreatedAtDesc(tenantId, pageable).map(mapper::toDto);
    }

    @Override
    public Page<TenantReviewDto> listByLandlord(String landlordId, Pageable pageable) {
        return trRepo.findByLandlord_UserIdOrderByCreatedAtDesc(landlordId, pageable).map(mapper::toDto);
    }

    @Override
    public Page<String> listPendingBookingsToRate(String landlordId, Pageable pageable) {
        return trRepo.findPendingBookingsForLandlord(landlordId, pageable);
    }
}
