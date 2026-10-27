package vn.edu.iuh.nhom01_www.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHoldDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private Long tripId;
    private UUID customerId;
    private List<Long> seatIds;
    private LocalDateTime holdExpiresAt;
}
