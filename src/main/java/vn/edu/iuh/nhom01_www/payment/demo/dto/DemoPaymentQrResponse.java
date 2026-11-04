package vn.edu.iuh.nhom01_www.payment.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoPaymentQrResponse {
    private Long bookingId;
    private String bookingCode;
    private String transactionId;
    private BigDecimal amount;
    private String amountFormatted;
    private String qrPayload;
    private String qrBase64;
    private long expiresInSeconds;
    private LocalDateTime expiresAt;
    private String status; // "PENDING"
    private String routeName;
    private String departureTimeFormatted;
    private List<String> seatNames;
    @Builder.Default
    private String demoNote = "THANH TOÁN MÔ PHỎNG — KHÔNG CHUYỂN TIỀN THẬT";
}
