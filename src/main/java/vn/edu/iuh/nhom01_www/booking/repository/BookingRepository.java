package vn.edu.iuh.nhom01_www.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.booking.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    java.util.List<Booking> findByTripId(Long tripId);
    java.util.List<Booking> findByStatusAndHoldExpiresAtBefore(String status, java.time.LocalDateTime time);
}
