package vn.edu.iuh.nhom01_www.payment.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoPaymentConfirmRequest {
    @NotNull(message = "Mã bookingId không được để trống!")
    private Long bookingId;

    private String transactionId;
}
