package vn.edu.iuh.nhom01_www.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.iuh.nhom01_www.booking.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    java.util.List<Booking> findByTripId(Long tripId);
    java.util.List<Booking> findByStatusAndHoldExpiresAtBefore(String status, java.time.LocalDateTime time);
    java.util.List<Booking> findByCustomerEmailOrderByCreatedAtDesc(String email);
    java.util.Optional<Booking> findByIdAndCustomerEmail(Long id, String email);
}
