package vn.edu.iuh.nhom01_www.payment.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmRequest;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;
import vn.edu.iuh.nhom01_www.payment.demo.processor.DemoPaymentProcessor;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/demo")
@RequiredArgsConstructor
public class DemoPaymentController {

    private final DemoPaymentProcessor demoPaymentProcessor;
    private final CurrentUserService currentUserService;

    @PostMapping("/create-qr")
    public ResponseEntity<ApiResponse<DemoPaymentQrResponse>> createDemoPaymentQr(
            @RequestBody Map<String, Object> body) {
        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;

        Long bookingId = null;
        if (body.get("bookingId") != null) {
            bookingId = Long.parseLong(body.get("bookingId").toString());
        }

        DemoPaymentQrResponse qrResponse = demoPaymentProcessor.createDemoPayment(bookingId, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tạo mã QR thanh toán demo thành công", qrResponse));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<DemoPaymentConfirmResponse>> confirmDemoPayment(
            @Valid @RequestBody DemoPaymentConfirmRequest request) {
        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;

        DemoPaymentConfirmResponse response = demoPaymentProcessor.processPayment(
                request.getBookingId(),
                request.getTransactionId(),
                customerId
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response.getMessage(), response));
    }

    @PostMapping("/cancel/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelDemoPayment(@PathVariable Long bookingId) {
        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;

        demoPaymentProcessor.cancelPayment(bookingId, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã hủy đơn đặt vé chờ thanh toán thành công", null));
    }
}
