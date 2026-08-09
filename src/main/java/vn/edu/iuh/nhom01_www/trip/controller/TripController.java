package vn.edu.iuh.nhom01_www.trip.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.trip.dto.TripRequest;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.service.TripService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Trip>>> searchTrips(
            @RequestParam String departureLocation,
            @RequestParam String arrivalLocation,
            @RequestParam String date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", tripService.searchTrips(departureLocation, arrivalLocation, date)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Trip>> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", tripService.getTripById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<Trip>> createTrip(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip created successfully", tripService.createTrip(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<Trip>> updateTrip(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip updated successfully", tripService.updateTrip(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TRIP_MANAGE') or hasAuthority('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<Trip>> updateTripStatus(@PathVariable Long id, @RequestBody Map<String, String> statusBody) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Status updated successfully", tripService.updateTripStatus(id, statusBody.get("status"))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Trip deleted successfully", null));
    }
}
