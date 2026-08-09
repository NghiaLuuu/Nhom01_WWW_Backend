package vn.edu.iuh.nhom01_www.booking.service;

import vn.edu.iuh.nhom01_www.booking.model.Booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    Booking createBooking(UUID customerId, Long tripId, List<Long> seatIds);
    void releaseExpiredBookings();
}
