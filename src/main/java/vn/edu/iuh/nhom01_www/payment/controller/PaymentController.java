package vn.edu.iuh.nhom01_www.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.payment.dto.PaymentRequest;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-link")
    public ResponseEntity<ApiResponse<String>> createPaymentLink(@Valid @RequestBody PaymentRequest request) {
        // Mock PayOS link generation
        String link = "https://payos.vn/mock-link/" + request.getBookingId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment link generated", link));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Payment>> handleWebhook(@RequestBody Map<String, Object> payload) {
        // Mock webhook parsing logic for PayOS structure
        Long bookingId = Long.parseLong(payload.getOrDefault("orderCode", "0").toString());
        String transactionId = payload.getOrDefault("transactionId", "").toString();
        String status = payload.getOrDefault("code", "FAILED").equals("00") ? "SUCCESS" : "FAILED";
        
        Payment payment = paymentService.processPaymentWebhook(bookingId, transactionId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Webhook processed", payment));
    }
}
