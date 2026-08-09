package vn.edu.iuh.nhom01_www.booking.dto;

import lombok.Data;

@Data
public class BookingRequest {
    @jakarta.validation.constraints.NotNull
    private Long tripId;
    @jakarta.validation.constraints.NotEmpty
    private java.util.List<Long> seatIds;
}
