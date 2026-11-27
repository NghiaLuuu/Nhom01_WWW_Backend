package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchResultDto {
    private Long tripId;
    private Long routeId;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private String departureTimeFormatted;
    private BigDecimal price;
    private String priceFormatted;
    private String vehicleType;
    private String licensePlate;
    private Integer totalSeats;
    private Integer availableSeats;
    private Boolean webBookingOpen;
    private String status;
}
