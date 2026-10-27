package vn.edu.iuh.nhom01_www.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.config.VNPayConfig;
import vn.edu.iuh.nhom01_www.payment.dto.PaymentRequest;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.service.PaymentService;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayConfig vnPayConfig;
    private final CurrentUserService currentUserService;

    @PostMapping("/vnpay/create-link")
    public ResponseEntity<ApiResponse<String>> createVNPayPaymentUrl(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest servletRequest) {
        Customer customer = currentUserService.getCurrentCustomer();
        String ipAddress = vnPayConfig.getIpAddress(servletRequest);
        String paymentUrl = paymentService.createVNPayPaymentUrl(request.getBookingId(), ipAddress, customer.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Tạo link thanh toán VNPay thành công", paymentUrl));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<ApiResponse<Payment>> handleVNPayReturn(@RequestParam Map<String, String> params) {
        Payment payment = paymentService.processVNPayCallback(params);
        String message = "SUCCESS".equals(payment.getStatus()) 
                ? "Thanh toán đơn hàng thành công!" 
                : "Thanh toán thất bại hoặc đã bị hủy!";
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS".equals(payment.getStatus()), message, payment));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<ApiResponse<Payment>> handleVNPayIPN(@RequestParam Map<String, String> params) {
        Payment payment = paymentService.processVNPayCallback(params);
        return ResponseEntity.ok(new ApiResponse<>(true, "IPN processed successfully", payment));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByBookingId(@PathVariable Long bookingId) {
        Payment payment = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", payment));
    }

    // Legacy/Generic payment link endpoint pointing to VNPay
    @PostMapping("/create-link")
    public ResponseEntity<ApiResponse<String>> createPaymentLink(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest servletRequest) {
        Customer customer = null;
        try {
            customer = currentUserService.getCurrentCustomer();
        } catch (Exception ignored) {}
        UUID customerId = customer != null ? customer.getId() : null;
        String ipAddress = vnPayConfig.getIpAddress(servletRequest);
        String link = paymentService.createVNPayPaymentUrl(request.getBookingId(), ipAddress, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment link generated", link));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Payment>> handleWebhook(@RequestBody Map<String, Object> payload) {
        Long bookingId = Long.parseLong(payload.getOrDefault("orderCode", payload.getOrDefault("bookingId", "0")).toString());
        String transactionId = payload.getOrDefault("transactionId", UUID.randomUUID().toString()).toString();
        String code = payload.getOrDefault("code", "FAILED").toString();
        String status = ("00".equals(code) || "SUCCESS".equalsIgnoreCase(code)) ? "SUCCESS" : "FAILED";
        
        Payment payment = paymentService.processPaymentWebhook(bookingId, transactionId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Webhook processed", payment));
    }
}
