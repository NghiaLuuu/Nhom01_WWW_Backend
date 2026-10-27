package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleOptionDto {

    private Long tripId;
    private LocalDateTime departureTime;
    private String departureLocation;
    private String arrivalLocation;
    private String vehicleType;
    private String licensePlate;
    private BigDecimal tripPrice;
    private int totalSeats;
    private int availableSeatsCount;
    private List<Long> availableSeatIds;

    // Chi tiết tài chính ước tính nếu khách hàng chọn chuyến này:
    private BigDecimal originalPrice;
    private BigDecimal rescheduleFee;      // Phí phạt No-Show
    private BigDecimal priceDifference;    // Chênh lệch giá nếu chuyến mới đắt hơn
    private BigDecimal totalAdditionalFee; // Tổng tiền phụ thu cần thanh toán
}
