package vn.edu.iuh.nhom01_www.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByVehicleId(Long vehicleId);
    List<Seat> findByVehicleIdOrderByFloorAscSeatNameAsc(Long vehicleId);
}
