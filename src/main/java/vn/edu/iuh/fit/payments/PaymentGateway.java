package vn.edu.iuh.fit.payments;

public interface PaymentGateway {
    String createPayment(String invoiceId, long amount, String description,
                         String returnUrl, String notifyUrl);
}
