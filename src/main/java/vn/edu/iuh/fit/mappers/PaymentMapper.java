package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.PaymentDto;
import vn.edu.iuh.fit.entities.Payment;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final BookingMapper bookingMapper;

    public PaymentDto toDto(Payment entity) {
        if (entity == null) return null;

        return new PaymentDto(
                entity.getPaymentId(),
                entity.getBooking() != null ? bookingMapper.toDto(entity.getBooking()) : null,
                entity.getAmount(),
                entity.getPaymentMethod(),
                entity.getPaymentStatus(),
                entity.getTransactionId(),
                entity.getRefundedAmount(),
                entity.getCreatedAt()
        );
    }

    public Payment toEntity(PaymentDto dto) {
        if (dto == null) return null;

        Payment entity = new Payment();
        entity.setPaymentId(dto.getPaymentId());
        entity.setAmount(dto.getAmount());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setTransactionId(dto.getTransactionId());
        entity.setRefundedAmount(dto.getRefundedAmount());
        entity.setCreatedAt(dto.getCreatedAt());

        // Booking sẽ được gán trong Service bằng cách fetch từ DB bằng bookingId
        return entity;
    }
}
