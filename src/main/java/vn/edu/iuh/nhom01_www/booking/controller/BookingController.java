package vn.edu.iuh.nhom01_www.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.booking.dto.BookingRequest;
import vn.edu.iuh.nhom01_www.booking.dto.CounterSaleRequest;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody BookingRequest request) {
        Customer customer = currentUserService.getCurrentCustomer();
        Booking booking = bookingService.createBooking(customer.getId(), request.getTripId(), request.getSeatIds());
        return ResponseEntity.ok(new ApiResponse<>(true, "Đặt vé giữ chỗ thành công (5 phút)", booking));
    }

    /**
     * Bán vé trực tiếp tại quầy dành cho Nhân viên / Thu ngân
     */
    @PostMapping("/counter-sale")
    @PreAuthorize("hasAuthority('MANAGE_TICKET') or hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Booking>> createCounterSale(@Valid @RequestBody CounterSaleRequest request) {
        UUID staffId = null;
        try {
            staffId = currentUserService.getCurrentStaff().getId();
        } catch (Exception ignored) {
        }
        Booking booking = bookingService.createCounterSaleBooking(request, staffId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bán vé tại quầy thành công!", booking));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyHistory() {
        if (currentUserService.isStaffOrAdmin()) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Success", bookingService.getAllBookings()));
        }
        Customer customer = currentUserService.getCurrentCustomer();
        List<Booking> bookings = bookingService.getMyBookingHistory(customer.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(@PathVariable Long id) {
        if (currentUserService.isStaffOrAdmin()) {
            Booking booking = bookingService.getBookingById(id, null);
            return ResponseEntity.ok(new ApiResponse<>(true, "Success", booking));
        }
        var customerOpt = currentUserService.getOptionalCurrentCustomer();
        UUID customerId = customerOpt.map(Customer::getId).orElse(null);
        Booking booking = bookingService.getBookingById(id, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", booking));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Booking>>> searchBookings(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String ticketCode) {
        Customer customer = null;
        try {
            customer = currentUserService.getCurrentCustomer();
        } catch (Exception ignored) {
            // Allow search by phone number or ticket code
        }
        UUID customerId = customer != null ? customer.getId() : null;
        List<Booking> bookings = bookingService.searchBookings(phoneNumber, ticketCode, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", bookings));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelPendingBooking(@PathVariable Long id) {
        UUID customerId = null;
        if (!currentUserService.isStaffOrAdmin()) {
            Customer customer = currentUserService.getCurrentCustomer();
            customerId = customer.getId();
        }
        Booking booking = bookingService.cancelPendingBooking(id, customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã hủy đơn đặt vé thành công", booking));
    }
}
