package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleTicketResponse {

    // Thông tin vé cũ (vé bị No-Show)
    private Long oldTicketId;
    private String oldTicketCode;
    private Long oldTripId;
    private String oldSeatName;

    // Thông tin vé mới cho chuyến tiếp theo
    private Long newTicketId;
    private String newTicketCode;
    private Long newTripId;
    private String newSeatName;
    private Integer newFloor;
    private LocalDateTime newDepartureTime;
    private String departureLocation;
    private String arrivalLocation;
    private String licensePlate;
    private String vehicleType;

    // Chi tiết tài chính / phụ thu
    private BigDecimal originalPrice;
    private BigDecimal newTripPrice;
    private BigDecimal rescheduleFee;      // Phí đổi vé No-show (ví dụ 20%)
    private BigDecimal priceDifference;    // Chênh lệch giá nếu chuyến mới đắt hơn
    private BigDecimal totalAdditionalFee; // Tổng phụ thu đã thanh toán

    private String status;
    private String qrCode;
    private String message;
}
