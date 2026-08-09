package vn.edu.iuh.nhom01_www.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    java.util.List<Seat> findByVehicleId(Long vehicleId);
}
