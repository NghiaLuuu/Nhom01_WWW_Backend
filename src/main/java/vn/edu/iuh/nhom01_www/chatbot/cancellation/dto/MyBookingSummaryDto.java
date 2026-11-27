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
public class MyBookingSummaryDto {
    private Long bookingId;
    private String bookingCode;
    private Long tripId;
    private String routeName;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private String departureTimeFormatted;
    private String vehicleType;
    private List<String> seatNames;
    private int ticketCount;
    private BigDecimal totalPrice;
    private String totalPriceFormatted;
    private String status;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private String createdAtFormatted;
    private boolean canViewTicket;
    private boolean isCancellable;
    private String nonCancellableReason;
}
