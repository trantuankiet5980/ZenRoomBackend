package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.services.BookingService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final BookingService bookingService;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody PaymentWebhookPayload payload,
                                          @RequestHeader(value="X-Signature", required=false) String sig) {
//         TODO: verify signature khi gắn PayOS/Sepay
        bookingService.handlePaymentWebhook(payload);
        return ResponseEntity.ok("OK");
    }
}
