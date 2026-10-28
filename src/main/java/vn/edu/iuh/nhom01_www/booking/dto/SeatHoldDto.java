package vn.edu.iuh.nhom01_www.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHoldDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long tripId;
    private Long seatId;
    private String seatName;
    private Long bookingId;
    private UUID customerId;
    private LocalDateTime holdExpiresAt;
}
