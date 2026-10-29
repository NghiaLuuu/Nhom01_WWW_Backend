package vn.edu.iuh.nhom01_www.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    java.util.List<Ticket> findByBookingTripId(Long tripId);
    List<Ticket> findByBookingId(Long bookingId);
}
