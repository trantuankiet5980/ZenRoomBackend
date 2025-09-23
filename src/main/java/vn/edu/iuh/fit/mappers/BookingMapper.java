package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.entities.Booking;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final PropertyMapper propertyMapper;
    private final UserMapper userMapper;
    private final DiscountCodeMapper discountCodeMapper;

    public BookingDto toDto(Booking entity) {
        if (entity == null) return null;
        return new BookingDto(
                entity.getBookingId(),
                propertyMapper.toDto(entity.getProperty()),
                userMapper.toDto(entity.getTenant()),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getBookingStatus(),
                entity.getTotalPrice(),
                entity.getNote(),
                entity.getPaymentUrl(),
                discountCodeMapper.toDto(entity.getDiscountCode()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null
        );
    }

    public Booking toEntity(BookingDto dto) {
        if (dto == null) return null;
        Booking entity = new Booking();
        entity.setBookingId(dto.getBookingId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setBookingStatus(dto.getBookingStatus());
        entity.setTotalPrice(dto.getTotalPrice());
        entity.setNote(dto.getNote());
        entity.setPaymentUrl(dto.getPaymentUrl());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        // Property, Tenant, DiscountCode, Contract sẽ được set trong service từ ID để tránh lazy/vòng lặp
        return entity;
    }
}
