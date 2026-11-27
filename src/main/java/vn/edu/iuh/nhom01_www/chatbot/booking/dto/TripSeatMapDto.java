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
public class TripSeatMapDto {
    private Long tripId;
    private String routeName;
    private String departureLocation;
    private String arrivalLocation;
    private String departureTimeFormatted;
    private String vehicleType;
    private BigDecimal unitPrice;
    private String unitPriceFormatted;
    private int passengerCount;
    private int totalSeats;
    private int availableCount;
    private int bookedCount;
    private int heldCount;
    private List<AvailableSeatDto> seats;
}
