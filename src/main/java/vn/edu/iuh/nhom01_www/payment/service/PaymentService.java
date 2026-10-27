package vn.edu.iuh.nhom01_www.payment.service;

import vn.edu.iuh.nhom01_www.payment.model.Payment;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    String createVNPayPaymentUrl(Long bookingId, String ipAddress, UUID customerId);
    Payment processVNPayCallback(Map<String, String> vnpParams);
    Payment processPaymentWebhook(Long bookingId, String transactionId, String status);
    Payment getPaymentByBookingId(Long bookingId);
}
