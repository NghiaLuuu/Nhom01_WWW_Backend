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
public class DepartureOptionDto {
    private String departureLocation;
    private String arrivalLocation;
    private String departureDate;
    private int activeTripCount;
    private int totalAvailableSeats;
    private BigDecimal minPrice;
    private String minPriceFormatted;
    private String earliestDepartureTimeFormatted;
    private List<String> vehicleTypes;
    private String command;
}
