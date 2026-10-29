package vn.edu.iuh.nhom01_www.seat.service;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import java.util.List;
import vn.edu.iuh.nhom01_www.seat.dto.SeatAvailabilityDto;
public interface SeatService {
    List<Seat> getSeatsByVehicleId(Long vehicleId);
    List<SeatAvailabilityDto> getSeatAvailabilityByTripId(Long tripId);
    List<Seat> bulkCreateSeats(Long vehicleId, SeatRequest request);
}
