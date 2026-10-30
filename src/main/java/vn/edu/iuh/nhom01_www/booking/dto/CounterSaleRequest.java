package vn.edu.iuh.nhom01_www.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterSaleRequest {
    @NotNull(message = "Vui lòng chọn chuyến xe!")
    private Long tripId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế!")
    private List<Long> seatIds;

    @NotBlank(message = "Họ tên khách hàng không được để trống!")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    private String customerPhone;

    private String customerEmail;

    private String paymentMethod; // Mặc định: CASH

    private String note;
}
