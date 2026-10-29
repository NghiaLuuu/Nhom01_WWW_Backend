package vn.edu.iuh.nhom01_www.seat.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.seat.service.SeatService;
import vn.edu.iuh.nhom01_www.seat.dto.SeatAvailabilityDto;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {
    private final SeatRepository seatRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;
    
    @Override
    public List<Seat> getSeatsByVehicleId(Long vehicleId) {
        return seatRepository.findByVehicleId(vehicleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatAvailabilityDto> getSeatAvailabilityByTripId(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));
        Map<Long, Ticket> ticketsBySeat = ticketRepository.findByBookingTripId(tripId).stream()
                .filter(ticket -> !"CANCELLED".equals(ticket.getStatus())
                        && !"RELEASED".equals(ticket.getStatus())
                        && ticket.getBooking() != null
                        && !"CANCELLED".equals(ticket.getBooking().getStatus())
                        && !"EXPIRED".equals(ticket.getBooking().getStatus())
                        && !ticket.getBooking().isHoldExpired())
                .collect(Collectors.toMap(ticket -> ticket.getSeat().getId(), Function.identity(),
                        (first, ignored) -> first));

        return seatRepository.findByVehicleId(trip.getVehicle().getId()).stream()
                .map(seat -> {
                    Ticket ticket = ticketsBySeat.get(seat.getId());
                    boolean held = ticket != null && "PENDING_PAYMENT".equals(ticket.getBooking().getStatus());
                    boolean booked = ticket != null && !held;
                    int floor = seat.getSeatName() != null
                            && seat.getSeatName().toUpperCase().startsWith("B") ? 2 : 1;
                    return new SeatAvailabilityDto(seat.getId(), seat.getSeatName(), seat.getSeatName(),
                            floor, booked, held);
                })
                .toList();
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
