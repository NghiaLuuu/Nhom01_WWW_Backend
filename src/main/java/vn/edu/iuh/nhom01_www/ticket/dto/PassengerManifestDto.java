package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerManifestDto {
    private Long ticketId;
    private String ticketCode;
    private String status;
    private Long seatId;
    private String seatName;
    private Integer floor;
    private String customerName;
    private String customerPhone;
    private Long bookingId;
    private BigDecimal price;
    private LocalDateTime usedAt;
}
