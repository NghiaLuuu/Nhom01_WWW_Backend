package vn.edu.iuh.nhom01_www.seat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.seat.dto.SeatAvailabilityDto;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.seat.service.SeatService;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRedisService seatHoldRedisService;

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

    @Override
    @Transactional(readOnly = true)
    public List<SeatAvailabilityDto> getSeatsByTripId(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));

        if (trip.getVehicle() == null) {
            return List.of();
        }

        Long vehicleId = trip.getVehicle().getId();
        List<Seat> seats = seatRepository.findByVehicleIdOrderByFloorAscSeatNameAsc(vehicleId);

        // Lấy danh sách ID ghế đã mua / đã xuất vé
        Set<Long> bookedSeatIds = new HashSet<>(ticketRepository.findBookedSeatIdsByTripId(tripId));

        // Lấy danh sách ID ghế đang được giữ chỗ tạm thời trong Redis
        Set<Long> heldSeatIds = new HashSet<>();
        try {
            if (seatHoldRedisService != null) {
                heldSeatIds.addAll(seatHoldRedisService.getHeldSeatIdsByTripId(tripId));
            }
        } catch (Exception ignored) {
            // Redis có thể chưa bật ở môi trường dev local, bỏ qua để không lỗi
        }

        return seats.stream().map(seat -> {
            boolean isBooked = bookedSeatIds.contains(seat.getId());
            boolean isHeld = heldSeatIds.contains(seat.getId());
            boolean isAvailable = !isBooked && !isHeld;

            return SeatAvailabilityDto.builder()
                    .id(seat.getId())
                    .seatName(seat.getSeatName())
                    .floor(seat.getFloor())
                    .isBooked(isBooked)
                    .isHeld(isHeld)
                    .isAvailable(isAvailable)
                    .build();
        }).toList();
    }
}
