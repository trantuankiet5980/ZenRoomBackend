package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import vn.edu.iuh.fit.dtos.LandlordYearlyPayoutDTO;
import vn.edu.iuh.fit.dtos.RevenueStatsDTO;
import vn.edu.iuh.fit.entities.Invoice;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {
    Invoice issueForBooking(String bookingId);
    Invoice markPaidByWebhook(String invoiceNo, String paymentRef);

    List<Object[]> getLandlordRevenueByDayRaw(LocalDate from, LocalDate to);
    List<Object[]> getLandlordRevenueByMonthRaw(Integer year, Integer month);
    List<Object[]> getLandlordRevenueByYearRaw(Integer year);

    RevenueStatsDTO getLandlordPayoutStats(String landlordId, Integer year, Integer month);
    Page<LandlordYearlyPayoutDTO> getLandlordYearlyPayout(Integer year, int page, int size);
}
