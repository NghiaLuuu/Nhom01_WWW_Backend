package vn.edu.iuh.nhom01_www.payment.demo.processor;

import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;

import java.util.UUID;

public interface PaymentProcessor {
    boolean supports(String paymentMethod);
    DemoPaymentQrResponse createDemoPayment(Long bookingId, UUID customerId);
    DemoPaymentConfirmResponse processPayment(Long bookingId, String transactionId, UUID customerId);
    void cancelPayment(Long bookingId, UUID customerId);
}
