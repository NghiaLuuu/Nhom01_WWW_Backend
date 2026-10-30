package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {
    private Long ticketId;
    private String ticketCode;
    private String status;
    private String seatName;
    private Integer floor;
    private String customerName;
    private String customerPhone;
    private Long bookingId;
    private Long tripId;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private String licensePlate;
    private String vehicleType;
    private BigDecimal price;
    private LocalDateTime usedAt;
    private String message;
}
