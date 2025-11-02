package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.entities.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {
    Invoice issueForBooking(String bookingId);
    Invoice markPaidByWebhook(String invoiceNo, String paymentRef, BigDecimal paidAmount);


    // InvoiceService.java
    List<Object[]> getLandlordRevenueByDayRaw(LocalDate from, LocalDate to);
    List<Object[]> getLandlordRevenueByMonthRaw(Integer year, Integer month);
    List<Object[]> getLandlordRevenueByYearRaw(Integer year);
}
