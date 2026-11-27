package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIntent {
    private String rawQuery;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDate departureDate;
    private String preferredTime;
    @Builder.Default
    private int passengerCount = 0;
    private String vehicleType;
    private List<String> seatPreferences;

    private Long targetTripId;
    private String targetTripTime;
    private List<Long> targetSeatIds;
    private List<String> targetSeatNames;
    private Long targetBookingId;
    private String targetTransactionId;

    @Builder.Default
    private boolean isConfirmation = false;
    @Builder.Default
    private boolean isRejection = false;
    @Builder.Default
    private boolean wantsAdjacentSeats = false;
    @Builder.Default
    private boolean isManualSeatSelection = false;
    @Builder.Default
    private boolean isPaymentSimulation = false;
    @Builder.Default
    private boolean isPromptInjection = false;
    private String promptInjectionReason;

    public boolean hasRouteInfo() {
        return departureLocation != null && !departureLocation.isBlank()
                && arrivalLocation != null && !arrivalLocation.isBlank();
    }

    public boolean hasDepartureDate() {
        return departureDate != null;
    }
}
