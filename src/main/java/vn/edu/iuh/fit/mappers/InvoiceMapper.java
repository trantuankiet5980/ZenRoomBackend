package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.InvoiceDto;
import vn.edu.iuh.fit.entities.Invoice;

@Component
public class InvoiceMapper {
    private final BookingMapper bookingMapper;

    public InvoiceMapper(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }
    public InvoiceDto toDto(Invoice e) {
        if (e == null) return null;
        return new InvoiceDto(
                e.getInvoiceId(),
                e.getInvoiceNo(),
                e.getStatus(),
                bookingMapper.toDto(e.getBooking()),
                e.getTenantName(),
                e.getTenantEmail(),
                e.getTenantPhone(),
                e.getLandlordName(),
                e.getLandlordEmail(),
                e.getLandlordPhone(),
                e.getPropertyTitle(),
                e.getPropertyAddressText(),
                e.getSubtotal(),
                e.getDiscount(),
                e.getTax(),
                e.getTotal(),
                e.getDueAmount(),
                e.getPaymentMethod(),
                e.getPaymentRef(),
                e.getPaymentUrl(),
                e.getQrPayload(),
                e.getPaidAt(),
                e.getIssuedAt(),
                e.getDueAt(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getItemsJson()
        );
    }

    public Invoice toEntity(InvoiceDto d) {
        if (d == null) return null;
        return Invoice.builder()
                .invoiceId(d.getInvoiceId())
                .invoiceNo(d.getInvoiceNo())
                .status(d.getStatus())
                .booking(bookingMapper.toEntity(d.getBooking()))
                .tenantName(d.getTenantName())
                .tenantEmail(d.getTenantEmail())
                .tenantPhone(d.getTenantPhone())
                .landlordName(d.getLandlordName())
                .landlordEmail(d.getLandlordEmail())
                .landlordPhone(d.getLandlordPhone())
                .propertyTitle(d.getPropertyTitle())
                .propertyAddressText(d.getPropertyAddressText())
                .subtotal(d.getSubtotal())
                .discount(d.getDiscount())
                .tax(d.getTax())
                .total(d.getTotal())
                .dueAmount(d.getDueAmount())
                .paymentMethod(d.getPaymentMethod())
                .paymentRef(d.getPaymentRef())
                .paymentUrl(d.getPaymentUrl())
                .qrPayload(d.getQrPayload())
                .paidAt(d.getPaidAt())
                .issuedAt(d.getIssuedAt())
                .dueAt(d.getDueAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .itemsJson(d.getItemsJson())
                .build();
    }

}
