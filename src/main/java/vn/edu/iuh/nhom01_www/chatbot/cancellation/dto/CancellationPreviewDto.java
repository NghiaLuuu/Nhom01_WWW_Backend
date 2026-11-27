package vn.edu.iuh.nhom01_www.chatbot.cancellation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPreviewDto {
    private String confirmationToken;
    private Long bookingId;
    private String bookingCode;
    private UUID customerId;
    private Long tripId;
    private String routeName;
    private String departureTimeFormatted;
    private String vehicleType;
    private List<Long> ticketIdsToCancel;
    private List<String> seatNamesToCancel;
    private int ticketCountToCancel;
    private boolean isPartialCancellation;
    private List<String> remainingSeatNames;
    private BigDecimal originalAmount;
    private String originalAmountFormatted;
    private int feePercent;
    private BigDecimal feeAmount;
    private String feeAmountFormatted;
    private BigDecimal refundAmount;
    private String refundAmountFormatted;
    private String refundMethod; // "DEMO_SIMULATION", "VNPay", "NONE"
    private String cancellationPolicy;
    private LocalDateTime expiresAt;
    private int expiresInSeconds;
    private String summaryMessage;
}
