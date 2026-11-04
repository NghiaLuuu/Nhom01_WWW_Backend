package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

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
public class BookingDraftDto {
    private String idempotencyKey;
    private UUID customerId;
    private Long tripId;
    private String routeName;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private String departureTimeFormatted;
    private List<Long> seatIds;
    private List<String> seatNames;
    private BigDecimal pricePerTicket;
    private BigDecimal totalPrice;
    private String totalPriceFormatted;
    private String vehicleType;
    private LocalDateTime expiresAt;
    @Builder.Default
    private boolean requiresConfirmation = true;
    private String summaryMessage;
}
