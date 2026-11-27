package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSessionState {
    private String sessionKey;
    @Builder.Default
    private String intent = "BOOK_TICKET";
    @Builder.Default
    private BookingStep step = BookingStep.COLLECTING_INFO;

    private String departureLocation;
    private String arrivalLocation;
    private LocalDate departureDate;
    private String preferredTime;
    private String vehicleType;
    @Builder.Default
    private int passengerCount = 1;

    private Long selectedTripId;
    @Builder.Default
    private List<Long> selectedSeatIds = new ArrayList<>();
    @Builder.Default
    private List<String> selectedSeatNames = new ArrayList<>();

    private Long selectedBookingId;
    @Builder.Default
    private List<Long> selectedTicketIds = new ArrayList<>();

    private String idempotencyKey;
    private Long bookingId;
    private String bookingCode;
    private String transactionId;
    private BigDecimal totalPrice;
    @Builder.Default
    private int stateVersion = 1;
    private LocalDateTime updatedAt;

    public String getDestination() {
        return arrivalLocation;
    }

    public void setDestination(String destination) {
        this.arrivalLocation = destination;
    }

    public void incrementStateVersion() {
        this.stateVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPassengerCount(int count) {
        if (count > 0) {
            this.passengerCount = count;
        }
    }

    public String getWorkflowStep() {
        return step != null ? step.name() : null;
    }

    public void setWorkflowStep(String workflowStep) {
        if (workflowStep != null) {
            try {
                this.step = BookingStep.valueOf(workflowStep);
            } catch (Exception ignored) {}
        }
    }

    public void revalidateSeatCount() {
        if (selectedSeatIds != null && selectedSeatIds.size() != passengerCount) {
            // Keep at most passengerCount
            if (selectedSeatIds.size() > passengerCount) {
                selectedSeatIds = new ArrayList<>(selectedSeatIds.subList(0, passengerCount));
                if (selectedSeatNames != null && selectedSeatNames.size() >= passengerCount) {
                    selectedSeatNames = new ArrayList<>(selectedSeatNames.subList(0, passengerCount));
                }
            }
        }
    }
}
