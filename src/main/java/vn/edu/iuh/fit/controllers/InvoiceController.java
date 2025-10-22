package vn.edu.iuh.fit.controllers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.InvoiceDto;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.mappers.InvoiceMapper;
import vn.edu.iuh.fit.repositories.InvoiceRepository;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceRepository invoiceRepo;
    private final InvoiceMapper invoiceMapper;

    @GetMapping("/tenant")
    public Page<InvoiceDto> tenantInvoivec(Principal principal,
                                           @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return invoiceRepo
                .findByBooking_Tenant_UserIdOrderByCreatedAtDesc(principal.getName(), pageRequest(page, size))
                .map(invoiceMapper::toDto);
    }
    @GetMapping("/tenant/{invoiceId}")
    public InvoiceDto tenantInvoiceDetail(@PathVariable String invoiceId, Principal principal) {
        return invoiceRepo.findByInvoiceIdAndBooking_Tenant_UserId(invoiceId, principal.getName())
                .map(invoiceMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @GetMapping("/tenant/booking/{bookingId}")
    public InvoiceDto tenantInvoiceByBooking(@PathVariable String bookingId, Principal principal) {
        return invoiceRepo.findByBooking_BookingIdAndBooking_Tenant_UserId(bookingId, principal.getName())
                .map(invoiceMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @GetMapping("/landlord")
    public Page<InvoiceDto> landlordInvoices(Principal principal,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return invoiceRepo
                .findByBooking_Property_Landlord_UserIdOrderByCreatedAtDesc(principal.getName(), pageRequest(page, size))
                .map(invoiceMapper::toDto);
    }

    @GetMapping("/landlord/{invoiceId}")
    public InvoiceDto landlordInvoiceDetail(@PathVariable String invoiceId, Principal principal) {
        return invoiceRepo.findByInvoiceIdAndBooking_Property_Landlord_UserId(invoiceId, principal.getName())
                .map(invoiceMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @GetMapping("/landlord/booking/{bookingId}")
    public InvoiceDto landlordInvoiceByBooking(@PathVariable String bookingId, Principal principal) {
        return invoiceRepo.findByBooking_BookingIdAndBooking_Property_Landlord_UserId(bookingId, principal.getName())
                .map(invoiceMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @PostMapping("/{invoiceId}/confirm-refund")
    public InvoiceDto confirmRefund(@PathVariable String invoiceId, Principal principal) {
        Invoice invoice = invoiceRepo.findByInvoiceIdAndBooking_Property_Landlord_UserId(invoiceId, principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.REFUND_PENDING) {
            throw new IllegalStateException("Hoá đơn không ở trạng thái chờ hoàn tiền");
        }

        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoice.setRefundConfirmed(Boolean.TRUE);
        invoice.setRefundConfirmedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());

        return invoiceMapper.toDto(invoiceRepo.save(invoice));
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return PageRequest.of(safePage, safeSize);
    }
}
