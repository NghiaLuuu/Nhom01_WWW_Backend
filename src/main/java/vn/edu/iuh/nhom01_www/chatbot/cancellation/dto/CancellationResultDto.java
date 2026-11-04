package vn.edu.iuh.nhom01_www.chatbot.cancellation.dto;

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
public class CancellationResultDto {
    private boolean success;
    private String message;
    private Long bookingId;
    private String bookingCode;
    private String bookingStatus;
    private List<String> cancelledSeats;
    private List<String> remainingSeats;
    private boolean isPartialCancellation;
    private BigDecimal refundAmount;
    private String refundAmountFormatted;
    private BigDecimal cancellationFee;
    private String cancellationFeeFormatted;
    private String refundStatus; // "REFUNDED", "REFUND_PENDING", "NO_REFUND_UNPAID", "REFUND_FAILED"
    private String refundTransactionId;
    private String refundMethod;
    private LocalDateTime cancelledAt;
    private String cancelledAtFormatted;
    private boolean alreadyExecuted;
}
