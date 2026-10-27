package vn.edu.iuh.nhom01_www.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.booking.model.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTripId(Long tripId);
    List<Booking> findByStatus(String status);
    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
    Optional<Booking> findByIdAndCustomer_Id(Long bookingId, UUID customerId);
    Optional<Booking> findByIdAndCustomerId(Long bookingId, UUID customerId);
    List<Booking> findByCustomer_PhoneNumberOrderByCreatedAtDesc(String phoneNumber);
    Optional<Booking> findDistinctByTickets_TicketCode(String ticketCode);
}
