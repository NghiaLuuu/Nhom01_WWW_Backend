package vn.edu.iuh.nhom01_www.seat.service;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import java.util.List;
public interface SeatService {
    List<Seat> getSeatsByVehicleId(Long vehicleId);
    List<Seat> bulkCreateSeats(Long vehicleId, SeatRequest request);
}
