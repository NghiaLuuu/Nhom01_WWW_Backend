package vn.edu.iuh.nhom01_www.cancelrequest.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

import java.util.List;
import java.util.Optional;

public interface CancelRequestRepository extends JpaRepository<CancelRequest, Long> {
    @EntityGraph(attributePaths = {"booking.customer", "booking.trip.route", "booking.tickets.seat", "staff"})
    List<CancelRequest> findByStatusOrderByCreatedAtAsc(String status);

    @EntityGraph(attributePaths = {"booking.customer", "booking.trip.route", "booking.tickets.seat", "staff"})
    List<CancelRequest> findAllByOrderByCreatedAtDesc();

    Optional<CancelRequest> findByBooking_Id(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CancelRequest r WHERE r.id = :id")
    Optional<CancelRequest> findByIdForUpdate(@Param("id") Long id);

}
