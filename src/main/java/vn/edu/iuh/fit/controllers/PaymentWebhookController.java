package vn.edu.iuh.fit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.PaymentConfirmationRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.dtos.requests.PayosWebhookPayload;
import vn.edu.iuh.fit.dtos.requests.SepayWebhookPayload;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.BookingService;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final BookingService bookingService;
    private final InvoiceRepository invoiceRepo;
    private final ObjectMapper objectMapper;

    private static final Pattern INV_PATTERN = Pattern.compile("(INV-[A-Z0-9\\-]+)");

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || body.isEmpty()) {
                return ResponseEntity.ok("IGNORED: EMPTY_BODY");
            }

            if (body.containsKey("data")) {
                PayosWebhookPayload payosPayload = objectMapper.convertValue(body, PayosWebhookPayload.class);
                return handlePayosWebhook(payosPayload);
            }
            SepayWebhookPayload sepayPayload = objectMapper.convertValue(body, SepayWebhookPayload.class);
            return handleSepayWebhook(sepayPayload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok("IGNORED: INVALID_PAYLOAD");
        } catch (Exception e) {
            return ResponseEntity.ok("IGNORED: EX " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPayment(@RequestBody PaymentConfirmationRequest request) {
        bookingService.confirmVirtualPayment(request);
        return ResponseEntity.ok("OK");
    }

    private ResponseEntity<String> handleSepayWebhook(SepayWebhookPayload body) {
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

        PaymentWebhookPayload payload = new PaymentWebhookPayload();
        payload.setInvoiceId(opt.get().getInvoiceId());
        payload.setAmount(body.getTransferAmount());
        payload.setSuccess(true);
        payload.setTransactionId(String.valueOf(body.getId()));

        bookingService.handlePaymentWebhook(payload);
        return ResponseEntity.ok("OK");
    }
    private ResponseEntity<String> handlePayosWebhook(PayosWebhookPayload payload) {
        System.out.println("PAYOS WEBHOOK: " + payload);
        if (payload == null || payload.getData() == null) {
            return ResponseEntity.ok("IGNORED: NO_DATA");
        }

        PayosWebhookPayload.PayosWebhookData data = payload.getData();

        Optional<Invoice> opt = Optional.empty();
        if (data.getTransactionId() != null && !data.getTransactionId().isBlank()) {
            opt = invoiceRepo.findByPaymentRef(data.getTransactionId());
        }
        if (opt.isEmpty() && data.getOrderCode() != null) {
            opt = invoiceRepo.findByPaymentRef(String.valueOf(data.getOrderCode()));
        }
        if (opt.isEmpty() && data.getPaymentLinkId() != null && !data.getPaymentLinkId().isBlank()) {
            opt = invoiceRepo.findByPaymentRef(data.getPaymentLinkId());
        }
        if (opt.isEmpty() && data.getDescription() != null && !data.getDescription().isBlank()) {
            opt = invoiceRepo.findByInvoiceNo(data.getDescription().trim());
        }

        if (opt.isEmpty()) {
            return ResponseEntity.ok("IGNORED: UNKNOWN_INVOICE");
        }

        Invoice invoice = opt.get();

        PaymentWebhookPayload internalPayload = new PaymentWebhookPayload();
        internalPayload.setInvoiceId(invoice.getInvoiceId());
        internalPayload.setAmount(data.getAmount() != null ? data.getAmount() : 0L);
        boolean success = isSuccessfulStatus(data.getStatus());
        if (!success) {
            success = isSuccessfulCode(payload.getCode(), payload.getDesc());
        }
        internalPayload.setSuccess(success);
        internalPayload.setTransactionId(resolveTransactionId(data));

        bookingService.handlePaymentWebhook(internalPayload);
        return ResponseEntity.ok("OK");
    }

    private boolean isSuccessfulStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return normalized.equals("PAID") || normalized.equals("SUCCESS") || normalized.equals("COMPLETED");
    }

    private boolean isSuccessfulCode(String code, String desc) {
        if (code != null && code.trim().equals("00")) {
            return true;
        }
        if (desc != null && desc.trim().equalsIgnoreCase("SUCCESS")) {
            return true;
        }
        return false;
    }

    private String resolveTransactionId(PayosWebhookPayload.PayosWebhookData data) {
        if (data.getTransactionId() != null && !data.getTransactionId().isBlank()) {
            return data.getTransactionId();
        }
        if (data.getPaymentLinkId() != null && !data.getPaymentLinkId().isBlank()) {
            return data.getPaymentLinkId();
        }
        if (data.getOrderCode() != null) {
            return String.valueOf(data.getOrderCode());
        }
        return null;
    }
}
