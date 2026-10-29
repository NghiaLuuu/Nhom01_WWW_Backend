package vn.edu.iuh.nhom01_www.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityDto {
    private Long id;
    private String seatName;
    private Integer floor;
    private Boolean isBooked;
    private Boolean isHeld;
    private Boolean isAvailable;
}

