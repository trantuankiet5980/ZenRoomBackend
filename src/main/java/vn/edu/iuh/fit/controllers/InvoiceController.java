package vn.edu.iuh.fit.controllers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PrePersist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.InvoiceDto;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.UserManagementLog;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.mappers.BookingMapper;
import vn.edu.iuh.fit.mappers.InvoiceMapper;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.repositories.UserManagementLogRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.InvoiceService;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceRepository invoiceRepo;
    private final InvoiceMapper invoiceMapper;
    private final RealtimeNotificationService realtimeNotificationService;
    private final UserManagementLogRepository userManagementLogRepository;
    private final AuthService authService;
    private final BookingMapper bookingMapper;
    private final InvoiceService invoiceService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<InvoiceDto> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) InvoiceStatus status,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before toDate");
        }

        int safePage = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size, 100));

        PageRequest pageRequest = PageRequest.of(safePage, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return invoiceRepo.findAllForAdmin(status, fromDate, toDate, pageRequest)
                .map(invoiceMapper::toDto);
    }

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
    public InvoiceDto confirmRefund(@PathVariable String invoiceId, Principal principal, Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        Invoice invoice;
        if (isAdmin) {
            invoice = invoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        } else {
            invoice = invoiceRepo.findByInvoiceIdAndBooking_Property_Landlord_UserId(invoiceId, principal.getName())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        }

        if (invoice.getStatus() != InvoiceStatus.REFUND_PENDING) {
            throw new IllegalStateException("Hoá đơn không ở trạng thái chờ hoàn tiền");
        }

        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoice.setRefundConfirmed(Boolean.TRUE);
        invoice.setRefundConfirmedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());

        invoice = invoiceRepo.save(invoice);

        var booking = invoice.getBooking();
        var bookingDto = booking != null ? bookingMapper.toDto(booking) : null;
        realtimeNotificationService.notifyRefundProcessed(bookingDto, invoice);

        if (isAdmin) {
            User admin = authService.getCurrentUser();
            if (admin != null) {
                User targetUser = booking != null ? booking.getTenant() : null;
                UserManagementLog log = UserManagementLog.builder()
                        .admin(admin)
                        .targetUser(targetUser)
                        .action("Đã hoàn tiền: invoice=" + invoice.getInvoiceNo()
                                + (booking != null ? ", booking=" + booking.getBookingId() : ""))
                        .createdAt(LocalDateTime.now())
                        .build();
                userManagementLogRepository.save(log);
            }
        }

        return invoiceMapper.toDto(invoice);
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return PageRequest.of(safePage, safeSize);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/landlords/daily")
    public List<Map<String, Object>> getLandlordDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        validateDateRange(from, to);
        var raw = invoiceService.getLandlordRevenueByDayRaw(from, to);
        return raw.stream().map(this::toDailyMap).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/landlords/monthly")
    public List<Map<String, Object>> getLandlordMonthlyRevenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        var raw = invoiceService.getLandlordRevenueByMonthRaw(year, month);
        return raw.stream().map(this::toMonthlyMap).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/landlords/yearly")
    public List<Map<String, Object>> getLandlordYearlyRevenue(
            @RequestParam(required = false) Integer year) {

        var raw = invoiceService.getLandlordRevenueByYearRaw(year);
        return raw.stream().map(this::toYearlyMap).toList();
    }

    private Map<String, Object> toDailyMap(Object[] row) {
        return Map.of(
                "landlordId", row[0],
                "landlordName", row[1],
                "date", row[2],
                "paidRevenue", row[3], // Tổng doanh thu đã thanh toán
                "refundedAmount", row[4], // Tổng số tiền đã hoàn trả
                "netRevenue", ((BigDecimal) row[3]).subtract((BigDecimal) row[4]) // Doanh thu thực tế
        );
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/stats/me/daily")
    public List<Map<String, Object>> getMyDailyRevenue(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        validateDateRange(from, to);
        var all = invoiceService.getLandlordRevenueByDayRaw(from, to);
        return all.stream()
                .filter(row -> principal.getName().equals(row[0]))
                .map(this::toDailyMap)
                .toList();
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/stats/me/monthly")
    public List<Map<String, Object>> getMyMonthlyRevenue(
            Principal principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        validateYearMonth(year, month);
        var all = invoiceService.getLandlordRevenueByMonthRaw(year, month);
        return all.stream()
                .filter(row -> principal.getName().equals(row[0]))
                .map(this::toMonthlyMap)
                .toList();
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/stats/me/yearly")
    public List<Map<String, Object>> getMyYearlyRevenue(
            Principal principal,
            @RequestParam(required = false) Integer year) {

        validateYear(year);
        var all = invoiceService.getLandlordRevenueByYearRaw(year);
        return all.stream()
                .filter(row -> principal.getName().equals(row[0]))
                .map(this::toYearlyMap)
                .toList();
    }

    private Map<String, Object> toMonthlyMap(Object[] row) {
        return Map.of(
                "landlordId", row[0],
                "landlordName", row[1],
                "year", row[2],
                "month", row[3],
                "paidRevenue", row[4], // Tổng doanh thu đã thanh toán
                "refundedAmount", row[5], // Tổng số tiền đã hoàn trả
                "netRevenue", ((BigDecimal) row[4]).subtract((BigDecimal) row[5]) // Doanh thu thực tế
        );
    }

    private Map<String, Object> toYearlyMap(Object[] row) {
        return Map.of(
                "landlordId", row[0],
                "landlordName", row[1],
                "year", row[2],
                "paidRevenue", row[3], // Tổng doanh thu đã thanh toán
                "refundedAmount", row[4], // Tổng số tiền đã hoàn trả
                "netRevenue", ((BigDecimal) row[3]).subtract((BigDecimal) row[4]) // Doanh thu thực tế
        );
    }


    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }
    }
    private void validateYear(Integer year) {
        if (year != null && (year < 1900 || year > 2100)) {
            throw new IllegalArgumentException("Invalid year");
        }
    }

    private void validateYearMonth(Integer year, Integer month) {
        validateYear(year);
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Invalid month");
        }
    }
}
