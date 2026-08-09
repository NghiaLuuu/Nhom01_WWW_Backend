package vn.edu.iuh.nhom01_www.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.booking.dto.BookingRequest;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody BookingRequest request) {
        // Mocking customerId extraction from JWT for now
        UUID customerId = UUID.randomUUID(); 
        Booking booking = bookingService.createBooking(customerId, request.getTripId(), request.getSeatIds());
        return ResponseEntity.ok(new ApiResponse<>(true, "Booking created temporarily", booking));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyHistory() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", Collections.emptyList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Booking>>> searchBookings(@RequestParam(required = false) String phoneNumber, @RequestParam(required = false) String ticketCode) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", Collections.emptyList()));
    }
}
