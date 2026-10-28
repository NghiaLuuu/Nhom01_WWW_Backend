package vn.edu.iuh.nhom01_www.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {
    @NotBlank(message = "Mã vé hoặc chuỗi QR không được để trống!")
    private String ticketCodeOrQr;

    private Long tripId;

    /**
     * Cho phép Nhân viên / Quản trị viên soát vé cưỡng chế (bỏ qua khung giờ soát vé)
     * dùng cho trường hợp ngoại lệ (chuyến xe bị hoãn/delay, hoặc phụ xe linh động cho khách lên).
     */
    private Boolean forceCheckIn;
}
