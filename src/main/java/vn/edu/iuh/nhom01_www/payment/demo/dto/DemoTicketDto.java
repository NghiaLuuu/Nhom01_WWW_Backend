package vn.edu.iuh.nhom01_www.payment.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoTicketDto {
    private Long ticketId;
    private String ticketCode;
    private String seatName;
    private Integer floor;
    private BigDecimal price;
    private String priceFormatted;
    private String status;
    private String qrPayload;
    private String qrCodeBase64;
}
