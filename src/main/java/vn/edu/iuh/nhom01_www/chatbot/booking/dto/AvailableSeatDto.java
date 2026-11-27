package vn.edu.iuh.nhom01_www.chatbot.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSeatDto {
    private Long seatId;
    private String seatName;
    private Integer floor;
    private String status; // "AVAILABLE", "BOOKED", "HELD"
    private BigDecimal price;
    private String priceFormatted;
}
