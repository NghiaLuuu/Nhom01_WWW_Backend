package vn.edu.iuh.nhom01_www.booking.service;

import vn.edu.iuh.nhom01_www.booking.model.Booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    Booking createBooking(UUID customerId, Long tripId, List<Long> seatIds);
    Booking completeBooking(UUID customerId, Long tripId, List<Long> seatIds);
    List<Booking> getMyHistory(String email);
    List<Booking> searchBookings(String phoneNumber, String ticketCode);
    Booking getBookingDetails(Long id, String email);
    void releaseExpiredBookings();
    List<Booking> getAllBookings();
}
