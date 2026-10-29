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
import org.springframework.security.core.Authentication;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody BookingRequest request,
                                                               Authentication authentication) {
        UUID customerId = getCustomerId(authentication);
        Booking booking = bookingService.createBooking(customerId, request.getTripId(), request.getSeatIds());
        return ResponseEntity.ok(new ApiResponse<>(true, "Booking created", booking));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Booking>> completeBooking(@Valid @RequestBody BookingRequest request,
                                                                 Authentication authentication) {
        Booking booking = bookingService.completeBooking(getCustomerId(authentication),
                request.getTripId(), request.getSeatIds());
        return ResponseEntity.ok(new ApiResponse<>(true, "Booking completed", booking));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyHistory(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                bookingService.getMyHistory(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBookingDetails(@PathVariable Long id,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                bookingService.getBookingDetails(id, authentication.getName())));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Booking>>> searchBookings(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String ticketCode) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                bookingService.searchBookings(phoneNumber, ticketCode)));
    }

    private UUID getCustomerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new vn.edu.iuh.nhom01_www.common.exception.CustomException(
                    "Please login before booking", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        Customer customer = customerRepository.findByEmail(authentication.getName());
        if (customer == null) {
            throw new vn.edu.iuh.nhom01_www.common.exception.CustomException(
                    "Customer account not found", org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return customer.getId();
    }
}
