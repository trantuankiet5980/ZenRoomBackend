package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.InvoiceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepo;
    private final BookingRepository bookingRepo;

    @Transactional
    @Override
    public Invoice issueForBooking(String bookingId) {
        var booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        // Không tạo trùng
        var existing = invoiceRepo.findByBooking_BookingId(bookingId);
        if (existing.isPresent()) return existing.get();

        // Tính tiền theo rule của bạn:
        // - Theo ngày: dueAmount = 50% * subtotal
        // - Theo tháng: dueAmount = tiền cọc = 1 tháng
        var subtotal = booking.getTotalPrice();// ví dụ đã tính sẵn
        var discount = BigDecimal.ZERO;          // nếu có mã giảm giá thì set
        var tax = BigDecimal.ZERO;               // tuỳ setup VAT
        var total = subtotal.subtract(discount).add(tax);

        BigDecimal dueAmount = computeDeposit(booking); // implement theo loại thuê

        var inv = Invoice.builder()
                .invoiceNo(generateInvoiceNo())
                .status(InvoiceStatus.ISSUED)
                .booking(booking)
                .tenantName(booking.getTenant().getFullName())
                .tenantEmail(booking.getTenant().getEmail())
                .tenantPhone(booking.getTenant().getPhoneNumber())
                .landlordName(booking.getProperty().getLandlord().getFullName())
                .landlordEmail(booking.getProperty().getLandlord().getEmail())
                .landlordPhone(booking.getProperty().getLandlord().getPhoneNumber())
                .propertyTitle(booking.getProperty().getTitle())
                .propertyAddressText(booking.getProperty().getAddress().getAddressFull())
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .total(total)
                .dueAmount(dueAmount)
                .issuedAt(LocalDateTime.now())
                .dueAt(booking.getCreatedAt().plusHours(24)) // ví dụ hạn TT 24h
                .paymentMethod("PAYOS")
                .itemsJson(buildItemsJson(booking)) // tuỳ bạn
                .build();

        return invoiceRepo.save(inv);
    }

    @Transactional
    public Invoice markPaidByWebhook(String invoiceNo, String paymentRef) {
        var inv = invoiceRepo.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        // Kiểm tra số tiền, tình trạng
        var paidAmount = inv.getTotal();
        if (paidAmount.compareTo(inv.getDueAmount()) < 0)
            throw new IllegalStateException("Paid amount less than due amount");
        inv.setStatus(InvoiceStatus.PAID);
        BigDecimal fee = paidAmount.multiply(new BigDecimal("0.03"))
                .setScale(2, RoundingMode.HALF_UP);
        inv.setPlatformFee(fee);
        inv.setLandlordReceivable(paidAmount.subtract(fee));
        inv.setPaymentRef(paymentRef);
        inv.setPaidAt(LocalDateTime.now());
        inv.setCancelledAt(null);
        return invoiceRepo.save(inv);
    }

    @Override
    public List<Object[]> getLandlordRevenueByDayRaw(LocalDate from, LocalDate to) {
        return invoiceRepo.getLandlordRevenueByDayRaw(from, to);
    }

    @Override
    public List<Object[]> getLandlordRevenueByMonthRaw(Integer year, Integer month) {
        return invoiceRepo.getLandlordRevenueByMonthRaw(year, month);
    }

    @Override
    public List<Object[]> getLandlordRevenueByYearRaw(Integer year) {
        return invoiceRepo.getLandlordRevenueByYearRaw(year);
    }

    @Override
    public RevenueStatsDTO getLandlordPayoutStats(String landlordId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();

        if (month != null) {
            validateMonth(month);
            BigDecimal total = invoiceRepo.getLandlordPayoutForMonth(landlordId, resolvedYear, month);
            var dailyBreakdown = invoiceRepo.getLandlordPayoutDailyBreakdown(landlordId, resolvedYear, month)
                    .stream()
                    .map(row -> new DailyRevenueDTO(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1]
                    ))
                    .toList();

            return new RevenueStatsDTO(
                    StatPeriod.MONTH,
                    resolvedYear,
                    month,
                    null,
                    total,
                    dailyBreakdown,
                    List.of(),
                    total,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        BigDecimal total = invoiceRepo.getLandlordPayoutForYear(landlordId, resolvedYear);
        var monthlyBreakdown = invoiceRepo.getLandlordPayoutMonthlyBreakdown(landlordId, resolvedYear)
                .stream()
                .map(row -> new MonthlyRevenueDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (BigDecimal) row[2]
                ))
                .toList();

        return new RevenueStatsDTO(
                StatPeriod.YEAR,
                resolvedYear,
                null,
                null,
                total,
                List.of(),
                monthlyBreakdown,
                total,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    @Override
    public Page<LandlordYearlyPayoutDTO> getLandlordYearlyPayout(Integer year, int page, int size) {
        int targetYear = year != null ? year : LocalDate.now().getYear();

        var rows = invoiceRepo.getLandlordPayoutByMonthForYear(targetYear);
        Map<String, LandlordYearlyPayoutDTO> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String landlordId = (String) row[0];
            String landlordName = (String) row[1];
            String landlordPhone = (String) row[2];
            int month = ((Number) row[3]).intValue();
            BigDecimal amount = (BigDecimal) row[4];
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }

            LandlordYearlyPayoutDTO dto = result.computeIfAbsent(landlordId, id ->
                    new LandlordYearlyPayoutDTO(
                            id,
                            landlordName,
                            landlordPhone,
                            new ArrayList<>(Collections.nCopies(12, BigDecimal.ZERO)),
                            BigDecimal.ZERO
                    ));

            dto.getMonthlyPayouts().set(month - 1, amount);
            dto.setTotal(dto.getTotal().add(amount));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageRequest = PageRequest.of(safePage, safeSize);

        List<LandlordYearlyPayoutDTO> allDtos = new ArrayList<>(result.values());
        int fromIndex = Math.min(safePage * safeSize, allDtos.size());
        int toIndex = Math.min(fromIndex + safeSize, allDtos.size());

        List<LandlordYearlyPayoutDTO> content = allDtos.subList(fromIndex, toIndex);
        return new PageImpl<>(content, pageRequest, allDtos.size());
    }

    @Override
    public LandlordRevenueSummaryDTO getLandlordProjectedRevenue(String landlordId, Integer year, Integer month, Integer day) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        Integer resolvedMonth = month;
        Integer resolvedDay = day;

        if (resolvedDay != null) {
            if (resolvedMonth == null) {
                resolvedMonth = today.getMonthValue();
            }
            LocalDate targetDate = validateDate(resolvedYear, resolvedMonth, resolvedDay);
            var invoices = invoiceRepo.findPaidByLandlordAndDate(landlordId, targetDate);

            List<LandlordRevenueBookingDTO> bookings = invoices.stream()
                    .map(this::toLandlordRevenueBooking)
                    .toList();

            BigDecimal totalReceivable = bookings.stream()
                    .map(LandlordRevenueBookingDTO::landlordReceivable)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPlatformFee = bookings.stream()
                    .map(LandlordRevenueBookingDTO::platformFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new LandlordRevenueSummaryDTO(
                    StatPeriod.DAY,
                    resolvedYear,
                    resolvedMonth,
                    resolvedDay,
                    totalReceivable,
                    totalPlatformFee,
                    List.of(new LandlordDailyRevenueDTO(targetDate, totalReceivable, totalPlatformFee)),
                    List.of(),
                    bookings
            );
        }

        if (resolvedMonth != null) {
            validateMonth(resolvedMonth);
            LocalDate from = LocalDate.of(resolvedYear, resolvedMonth, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
            var rows = invoiceRepo.getLandlordRevenueByDayRaw(from, to);

            List<LandlordDailyRevenueDTO> daily = rows.stream()
                    .filter(row -> landlordId.equals(row[0]))
                    .map(row -> new LandlordDailyRevenueDTO(
                            ((java.sql.Date) row[2]).toLocalDate(),
                            safeAmount(row[5]),
                            safeAmount(row[4])
                    ))
                    .toList();

            BigDecimal totalReceivable = daily.stream()
                    .map(LandlordDailyRevenueDTO::landlordReceivable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPlatformFee = daily.stream()
                    .map(LandlordDailyRevenueDTO::platformFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new LandlordRevenueSummaryDTO(
                    StatPeriod.MONTH,
                    resolvedYear,
                    resolvedMonth,
                    null,
                    totalReceivable,
                    totalPlatformFee,
                    daily,
                    List.of(),
                    List.of()
            );
        }

        var rows = invoiceRepo.getLandlordRevenueByMonthRaw(resolvedYear, null);
        List<LandlordMonthlyRevenueDTO> monthly = rows.stream()
                .filter(row -> landlordId.equals(row[0]))
                .map(row -> new LandlordMonthlyRevenueDTO(
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue(),
                        safeAmount(row[6]),
                        safeAmount(row[5])
                ))
                .toList();

        BigDecimal totalReceivable = monthly.stream()
                .map(LandlordMonthlyRevenueDTO::landlordReceivable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFee = monthly.stream()
                .map(LandlordMonthlyRevenueDTO::platformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LandlordRevenueSummaryDTO(
                StatPeriod.YEAR,
                resolvedYear,
                null,
                null,
                totalReceivable,
                totalPlatformFee,
                List.of(),
                monthly,
                List.of()
        );
    }

    @Override
    public AdminLandlordMonthlyPayoutDTO getLandlordMonthlyPayout(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();
        validateMonth(resolvedMonth);

        var rows = invoiceRepo.getLandlordRevenueByMonthRaw(resolvedYear, resolvedMonth);
        List<LandlordMonthlyPayoutDTO> landlords = rows.stream()
                .map(row -> new LandlordMonthlyPayoutDTO(
                        (String) row[0],
                        (String) row[1],
                        safeAmount(row[4]),
                        safeAmount(row[5]),
                        safeAmount(row[6])
                ))
                .toList();

        BigDecimal totalPaid = landlords.stream()
                .map(LandlordMonthlyPayoutDTO::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFee = landlords.stream()
                .map(LandlordMonthlyPayoutDTO::platformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceivable = landlords.stream()
                .map(LandlordMonthlyPayoutDTO::landlordReceivable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminLandlordMonthlyPayoutDTO(
                resolvedYear,
                resolvedMonth,
                totalPaid,
                totalPlatformFee,
                totalReceivable,
                landlords
        );
    }

    @Override
    public LandlordRevenueSummaryDTO getLandlordMonthlyRevenueDetail(String landlordId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();
        validateMonth(resolvedMonth);

        var invoices = invoiceRepo.findPaidByLandlordAndMonth(landlordId, resolvedYear, resolvedMonth);
        List<LandlordRevenueBookingDTO> bookings = invoices.stream()
                .map(this::toLandlordRevenueBooking)
                .toList();

        BigDecimal totalReceivable = bookings.stream()
                .map(LandlordRevenueBookingDTO::landlordReceivable)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlatformFee = bookings.stream()
                .map(LandlordRevenueBookingDTO::platformFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LandlordRevenueSummaryDTO(
                StatPeriod.MONTH,
                resolvedYear,
                resolvedMonth,
                null,
                totalReceivable,
                totalPlatformFee,
                List.of(),
                List.of(),
                bookings
        );
    }

    private LandlordRevenueBookingDTO toLandlordRevenueBooking(Invoice invoice) {
        return new LandlordRevenueBookingDTO(
                invoice.getInvoiceId(),
                invoice.getInvoiceNo(),
                invoice.getBooking() != null ? invoice.getBooking().getBookingId() : null,
                invoice.getBooking() != null && invoice.getBooking().getProperty() != null
                        ? invoice.getBooking().getProperty().getTitle()
                        : null,
                invoice.getTotal(),
                invoice.getPlatformFee(),
                invoice.getLandlordReceivable(),
                invoice.getPaidAt() != null ? invoice.getPaidAt().toLocalDate() : null
        );
    }

    private LocalDate validateDate(int year, int month, int day) {
        validateMonth(month);
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date", e);
        }
    }

    private BigDecimal safeAmount(Object value) {
        return value != null ? (BigDecimal) value : BigDecimal.ZERO;
    }

    private BigDecimal computeDeposit(Booking booking) {
        // Ví dụ:
        // if (booking.getType() == DAILY) return booking.getTotalAmount().multiply(new BigDecimal("0.5"));
        // if (booking.getType() == MONTHLY) return booking.getMonthlyPrice(); // cọc 1 tháng
        return booking.getTotalPrice(); // tạm cho MVP
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month");
        }
    }

    private String generateInvoiceNo() {
        // Pattern: ZR-YYYYMM-xxxxx tăng dần
        var prefix = "ZR-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now()) + "-";
        var max = invoiceRepo.findMaxInvoiceNoWithPrefix(prefix); // ví dụ: ZR-202509-000123
        int next = 1;
        if (max != null) {
            var tail = max.substring(prefix.length());
            next = Integer.parseInt(tail) + 1;
        }
        return prefix + String.format("%05d", next);
    }

    private String buildItemsJson(Booking b) {
        // Tối giản: 1 dòng mô tả
        // Có thể dùng Jackson ObjectMapper để serialize list item
        var map = Map.of(
                "items", List.of(Map.of(
                        "name", "Đặt phòng: " + b.getProperty().getTitle(),
                        "qty", 1,
                        "price", b.getTotalPrice(),
                        "amount", b.getTotalPrice()
                ))
        );
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
