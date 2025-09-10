package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.entities.Invoice;

import java.math.BigDecimal;

public interface InvoiceService {
    Invoice issueForBooking(String bookingId);
    Invoice markPaidByWebhook(String invoiceNo, String paymentRef, BigDecimal paidAmount);
}
