package vn.edu.iuh.nhom01_www.seat.service;

import vn.edu.iuh.nhom01_www.seat.dto.SeatAvailabilityDto;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

import java.util.List;

public interface SeatService {
    List<Seat> getSeatsByVehicleId(Long vehicleId);
    List<Seat> bulkCreateSeats(Long vehicleId, SeatRequest request);
    List<SeatAvailabilityDto> getSeatsByTripId(Long tripId);
}
