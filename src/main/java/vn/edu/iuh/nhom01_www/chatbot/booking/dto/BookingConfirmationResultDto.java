package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

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
public class BookingConfirmationResultDto {
    private Long bookingId;
    private String bookingCode;
    private String status; // PENDING_PAYMENT, PAID, CANCELLED, FAILED
    private BigDecimal totalPrice;
    private String totalPriceFormatted;
    private Long tripId;
    private String routeName;
    private String departureTimeFormatted;
    private List<String> seatNames;
    private int holdMinutes;
    private String paymentInstructions;
    private boolean isAlreadyExisting;
}
