package vn.edu.iuh.nhom01_www.seat.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.seat.dto.SeatRequest;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.service.SeatService;
import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;
    
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByVehicleId(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", seatService.getSeatsByVehicleId(vehicleId)));
    }
    
    @PostMapping("/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<List<Seat>>> createSeats(@PathVariable Long vehicleId, @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Seats created", seatService.bulkCreateSeats(vehicleId, request)));
    }
}
