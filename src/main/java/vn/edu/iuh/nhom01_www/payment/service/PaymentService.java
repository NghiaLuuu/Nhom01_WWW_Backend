package vn.edu.iuh.nhom01_www.payment.service;

import vn.edu.iuh.nhom01_www.payment.model.Payment;

public interface PaymentService {
    Payment processPaymentWebhook(Long bookingId, String transactionId, String status);
}
