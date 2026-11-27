package vn.edu.iuh.nhom01_www.chatbot.cancellation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationEligibilityDto {
    private boolean eligible;
    private String reason;
    private Long bookingId;
    private String bookingCode;
    private double hoursUntilDeparture;
    private int feePercent;
    private BigDecimal cancellationFee;
    private String cancellationFeeFormatted;
    private BigDecimal refundAmount;
    private String refundAmountFormatted;
    private String policyDescription;
    private boolean isPaid;
    private boolean requiresStaffApproval;
    private List<Long> cancellableTicketIds;
    private List<String> cancellableSeatNames;
}
