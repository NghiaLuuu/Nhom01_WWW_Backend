package vn.edu.iuh.nhom01_www.seat.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.seat.service.SeatService;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {
    private final SeatRepository seatRepository;
    private final VehicleRepository vehicleRepository;
    
    @Override
    public List<Seat> getSeatsByVehicleId(Long vehicleId) {
        return seatRepository.findByVehicleId(vehicleId);
    }
    
    @Override
    public List<Seat> bulkCreateSeats(Long vehicleId, SeatRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));
        
        List<Seat> existing = seatRepository.findByVehicleId(vehicleId);
        if (!existing.isEmpty()) {
            throw new CustomException("Seats already configured for this vehicle", HttpStatus.BAD_REQUEST);
        }
        
        List<Seat> seats = new ArrayList<>();
        int half = request.getTotalSeats() / 2;
        for (int i = 1; i <= request.getTotalSeats(); i++) {
            String prefix = i <= half ? "A" : "B";
            int num = i <= half ? i : (i - half);
            String seatName = prefix + String.format("%02d", num);
            int floor = i <= half ? 1 : 2;
            seats.add(Seat.builder().vehicle(vehicle).seatName(seatName).floor(floor).build());
        }
        return seatRepository.saveAll(seats);
    }
}
