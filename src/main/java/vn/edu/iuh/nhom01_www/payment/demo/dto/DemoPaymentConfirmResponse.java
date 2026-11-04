package vn.edu.iuh.nhom01_www.payment.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoPaymentConfirmResponse {
    private boolean success;
    private Long bookingId;
    private String bookingCode;
    private String status; // "PAID"
    private BigDecimal totalPrice;
    private String totalPriceFormatted;
    private String paymentMethod;
    private String transactionId;
    private String routeName;
    private String departureTimeFormatted;
    private List<DemoTicketDto> tickets;
    private String message;
}
