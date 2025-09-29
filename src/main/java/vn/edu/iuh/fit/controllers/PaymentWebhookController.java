package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.dtos.requests.SepayWebhookPayload;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.BookingService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final BookingService bookingService;
    private final InvoiceRepository invoiceRepo;

    // Regex bắt mã hoá đơn kiểu: INV-20250924-ABCDEFG
    private static final Pattern INV_PATTERN = Pattern.compile("(INV-[A-Z0-9\\-]+)");

    @PostMapping("/webhook")
    public ResponseEntity<String> sepayWebhook(@RequestBody SepayWebhookPayload body) {
        try {
            System.out.println("SEPAY WEBHOOK: " + body);
            if (!"in".equalsIgnoreCase(body.getTransferType())) {
                return ResponseEntity.ok("IGNORED: NOT_IN");
            }

            String invoiceNo = null;
            if (body.getCode() != null && !body.getCode().isBlank()) {
                invoiceNo = body.getCode().trim();
            } else if (body.getContent() != null) {
                Matcher m = INV_PATTERN.matcher(body.getContent().toUpperCase());
                if (m.find()) invoiceNo = m.group(1);
            }

            if (invoiceNo == null) {
                return ResponseEntity.ok("IGNORED: NO_INVOICE_NO");
            }

            Optional<Invoice> opt = invoiceRepo.findByInvoiceNo(invoiceNo);
            if (opt.isEmpty()) {
                return ResponseEntity.ok("IGNORED: UNKNOWN_INVOICE");
            }

            // map về payload cũ
            PaymentWebhookPayload payload = new PaymentWebhookPayload();
            payload.setInvoiceId(opt.get().getInvoiceId());
            payload.setAmount(body.getTransferAmount());
            payload.setSuccess(true);
            payload.setTransactionId(String.valueOf(body.getId()));

            bookingService.handlePaymentWebhook(payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.ok("IGNORED: EX " + e.getMessage());
        }
    }
}
