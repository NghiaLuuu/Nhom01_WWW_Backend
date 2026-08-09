package vn.edu.iuh.nhom01_www.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    @jakarta.validation.constraints.NotNull
    private Long bookingId;
}
