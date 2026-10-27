package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleTicketRequest {

    /**
     * ID chuyến xe mới muốn chuyển sang.
     * Nếu để trống (null), hệ thống sẽ tự động chọn chuyến xe tiếp theo gần nhất còn chỗ của cùng tuyến đường.
     */
    private Long newTripId;

    /**
     * ID ghế ngồi mới muốn chọn trên chuyến mới.
     * Nếu để trống (null), hệ thống sẽ tự động chọn ghế trống đầu tiên hợp lệ.
     */
    private Long newSeatId;

    /**
     * Phương thức thanh toán phần phụ phí (Phí đổi vé No-show + Chênh lệch giá nếu có):
     * "CASH", "VNPAY", "TRANSFER",... (Mặc định: "CASH").
     */
    private String paymentMethod;

    /**
     * Ghi chú bổ sung (tùy chọn).
     */
    private String note;
}
