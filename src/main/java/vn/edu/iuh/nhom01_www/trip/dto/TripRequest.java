package vn.edu.iuh.nhom01_www.trip.dto;

import lombok.Data;

@Data
public class TripRequest {
    @jakarta.validation.constraints.NotNull
    private Long routeId;
    @jakarta.validation.constraints.NotNull
    private Long vehicleId;
    @jakarta.validation.constraints.NotNull
    private java.time.LocalDateTime departureTime;
    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal price;
}
