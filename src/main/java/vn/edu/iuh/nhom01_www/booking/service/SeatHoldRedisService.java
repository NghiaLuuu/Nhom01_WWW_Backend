package vn.edu.iuh.nhom01_www.booking.service;

import vn.edu.iuh.nhom01_www.booking.dto.BookingHoldDto;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRedisService {

    /**
     * Thử giữ chỗ một danh sách ghế trên Redis với thời hạn TTL (giây).
     * @return Danh sách tên các ghế đã bị người khác giữ nếu thất bại, hoặc List rỗng nếu thành công.
     */
    List<String> tryHoldSeats(Long tripId, List<Seat> seats, Long bookingId, UUID customerId, long durationInSeconds);

    /**
     * Lấy danh sách các seatId đang được giữ chỗ trong Redis của một chuyến xe.
     */
    List<Long> getHeldSeatIdsByTripId(Long tripId);

    /**
     * Kiểm tra xem đơn giữ chỗ (Booking) có còn tồn tại / còn hiệu lực trên Redis không.
     */
    boolean isBookingHoldActive(Long bookingId);

    /**
     * Lấy thông tin chi tiết giữ chỗ BookingHoldDto từ Redis.
     */
    Optional<BookingHoldDto> getBookingHold(Long bookingId);

    /**
     * Lấy thời điểm hết hạn giữ chỗ (holdExpiresAt) từ Redis.
     */
    Optional<LocalDateTime> getHoldExpiresAt(Long bookingId);

    /**
     * Lấy thời gian giữ chỗ còn lại (tính bằng giây) từ TTL Redis.
     */
    long getRemainingHoldSeconds(Long bookingId);

    /**
     * Giải phóng các ghế đã giữ của một booking trên Redis (xóa các key hold).
     */
    void releaseHoldByBookingId(Long bookingId);

    /**
     * Giải phóng các ghế cụ thể của một chuyến xe trên Redis.
     */
    void releaseHoldBySeats(Long tripId, List<Long> seatIds);
}
