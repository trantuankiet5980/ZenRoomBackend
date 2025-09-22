package vn.edu.iuh.fit.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.BookingService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/fake")
@RequiredArgsConstructor
public class PaymentSimulationController {
    private final FakePaymentGateway fakePaymentGateway;
    private final InvoiceRepository invoiceRepo;
    private final BookingService bookingService;

    @GetMapping("/pay")
    public ResponseEntity<?> payPage(@RequestParam String invoiceId,
                                     @RequestParam String token,
                                     @RequestParam(required=false) String returnUrl,
                                     @RequestParam(required=false) String notifyUrl) {
        String mapped = fakePaymentGateway.consumeToken(token);
        if (mapped == null || !mapped.equals(invoiceId))
            return ResponseEntity.badRequest().body(Map.of("error","Invalid/expired token"));

        Invoice inv = invoiceRepo.findById(invoiceId).orElse(null);
        if (inv == null) return ResponseEntity.badRequest().body(Map.of("error","Invoice not found"));

        String html = """
      <html><body>
      <h2>Fake Payment Gateway</h2>
      <p>Invoice: %s</p>
      <p>Amount: %s</p>
      <form method='post' action='/api/v1/payments/fake/confirm'>
        <input type='hidden' name='invoiceId' value='%s'/>
        <button type='submit'>Simulate Pay (SUCCESS)</button>
      </form>
      <form method='post' action='/api/v1/payments/fake/confirm?fail=true'>
        <input type='hidden' name='invoiceId' value='%s'/>
        <button type='submit'>Simulate Pay (FAILED)</button>
      </form>
      </body></html>
      """.formatted(inv.getInvoiceId(), inv.getDueAmount(), inv.getInvoiceId(), inv.getInvoiceId());

        return ResponseEntity.ok().header("Content-Type","text/html").body(html);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam String invoiceId,
                                     @RequestParam(defaultValue="false") boolean fail) {
        var payload = new PaymentWebhookPayload();
        payload.setInvoiceId(invoiceId);
        payload.setSuccess(!fail);
        payload.setTransactionId("FAKE-"+System.currentTimeMillis());
        bookingService.handlePaymentWebhook(payload);
        return ResponseEntity.ok("OK");
    }
}
