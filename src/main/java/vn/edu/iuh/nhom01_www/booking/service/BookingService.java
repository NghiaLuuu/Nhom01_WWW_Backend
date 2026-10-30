package vn.edu.iuh.nhom01_www.booking.service;

import vn.edu.iuh.nhom01_www.booking.dto.CounterSaleRequest;
import vn.edu.iuh.nhom01_www.booking.model.Booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    Booking createBooking(UUID customerId, Long tripId, List<Long> seatIds);
    void releaseExpiredBookings();
    List<Booking> getAllBookings();
    List<Booking> getMyBookingHistory(UUID customerId);
    Booking getBookingById(Long bookingId, UUID customerId);
    List<Booking> searchBookings(String phoneNumber, String ticketCode, UUID customerId);
    Booking cancelPendingBooking(Long bookingId, UUID customerId);

    // Bán vé trực tiếp tại quầy cho khách vãng lai (POS counter sale)
    Booking createCounterSaleBooking(CounterSaleRequest request, UUID staffId);
}
