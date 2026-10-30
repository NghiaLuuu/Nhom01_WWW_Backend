package vn.edu.iuh.nhom01_www.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBookingId(Long bookingId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.booking.id = :bookingId order by t.id")
    List<Ticket> findForUpdateByBookingId(@Param("bookingId") Long bookingId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.status = 'USED', t.usedAt = :usedAt, t.checkedBy = :checkedBy " +
           "WHERE t.id = :ticketId AND t.status IN ('UNUSED', 'ISSUED')")
    int markUsedIfUnused(@Param("ticketId") Long ticketId,
                         @Param("usedAt") LocalDateTime usedAt,
                         @Param("checkedBy") Staff checkedBy);

    @Query("SELECT t.seat.id FROM Ticket t " +
           "WHERE t.booking.trip.id = :tripId " +
           "AND t.status IN ('ISSUED', 'RESERVED', 'USED')")
    List<Long> findBookedSeatIdsByTripId(@Param("tripId") Long tripId);

    @Query("SELECT t FROM Ticket t WHERE t.seat.id IN :seatIds AND t.booking.trip.id = :tripId " +
           "AND t.status IN ('ISSUED', 'RESERVED', 'USED')")
    List<Ticket> findBookedTicketsByTripIdAndSeatIds(@Param("tripId") Long tripId,
                                                    @Param("seatIds") List<Long> seatIds);

    @Query("SELECT t FROM Ticket t " +
           "JOIN FETCH t.seat s " +
           "JOIN FETCH t.booking b " +
           "JOIN FETCH b.customer c " +
           "WHERE b.trip.id = :tripId " +
           "AND t.status IN :statuses " +
           "ORDER BY s.floor ASC, s.seatName ASC")
    List<Ticket> findPassengerManifestByTripId(@Param("tripId") Long tripId,
                                              @Param("statuses") List<String> statuses);

    @Query("SELECT t FROM Ticket t " +
           "JOIN FETCH t.booking b " +
           "JOIN FETCH b.trip tr " +
           "WHERE t.status = 'ISSUED' " +
           "AND tr.departureTime <= :gateCloseCutoff " +
           "AND tr.status NOT IN ('CANCELLED')")
    List<Ticket> findIssuedTicketsPastGateClosure(@Param("gateCloseCutoff") LocalDateTime gateCloseCutoff);

    @Query("SELECT t FROM Ticket t " +
           "WHERE t.booking.trip.id = :tripId " +
           "AND t.status = 'ISSUED'")
    List<Ticket> findIssuedTicketsByTripId(@Param("tripId") Long tripId);
}
