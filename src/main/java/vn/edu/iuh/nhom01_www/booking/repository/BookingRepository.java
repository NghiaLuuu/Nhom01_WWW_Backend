package vn.edu.iuh.nhom01_www.booking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.nhom01_www.booking.model.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status IN ('PAID', 'COMPLETED', 'PARTIALLY_CANCELLED')")
    BigDecimal sumPaidAndPartialBookingTotals();

    @Query("SELECT COALESCE(SUM(t.price), 0) FROM Ticket t WHERE t.booking.status = 'PARTIALLY_CANCELLED' AND t.status = 'CANCELLED'")
    BigDecimal sumCancelledTicketsInPartialBookings();
    List<Booking> findByTripId(Long tripId);
    List<Booking> findByStatus(String status);
    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
    Optional<Booking> findByIdAndCustomer_Id(Long bookingId, UUID customerId);
    Optional<Booking> findByIdAndCustomerId(Long bookingId, UUID customerId);
    List<Booking> findByCustomer_PhoneNumberOrderByCreatedAtDesc(String phoneNumber);
    Optional<Booking> findDistinctByTickets_TicketCode(String ticketCode);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.customer c LEFT JOIN FETCH b.trip t ORDER BY b.createdAt DESC")
    List<Booking> findAllRecent(Pageable pageable);
}
