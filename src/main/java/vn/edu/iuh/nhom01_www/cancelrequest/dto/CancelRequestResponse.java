package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Explicit response shape: never serialize customer/staff credentials or lazy entity graphs. */
public record CancelRequestResponse(
        Long id, BookingSummary booking, String reason, String status, String rejectReason,
        LocalDateTime createdAt, LocalDateTime resolvedAt, StaffSummary staff) {

    public static CancelRequestResponse from(CancelRequest request) {
        String reason = request.getReason();
        if ((reason == null || reason.isBlank()) && !"REJECTED".equals(request.getStatus())) {
            // Existing requests stored the customer's reason in rejectReason.
            reason = request.getRejectReason();
        }
        Staff staff = request.getStaff();
        return new CancelRequestResponse(request.getId(), bookingSummary(request.getBooking()), reason,
                request.getStatus(), request.getRejectReason(), request.getCreatedAt(), request.getResolvedAt(),
                staff == null ? null : new StaffSummary(staff.getId(), staff.getFullName(), staff.getStaffCode()));
    }

    private static BookingSummary bookingSummary(Booking booking) {
        Customer customer = booking.getCustomer();
        Trip trip = booking.getTrip();
        RouteSummary route = trip == null || trip.getRoute() == null ? null : new RouteSummary(
                trip.getRoute().getId(), trip.getRoute().getDepartureLocation(), trip.getRoute().getArrivalLocation());
        return new BookingSummary(booking.getId(), booking.getBookingCode(), booking.getStatus(),
                booking.getTotalPrice(), booking.getSeats(), booking.getCreatedAt(),
                customer == null ? null : new CustomerSummary(customer.getId(), customer.getFullName(),
                        customer.getEmail(), customer.getPhoneNumber()),
                trip == null ? null : new TripSummary(trip.getId(), trip.getDepartureTime(), route));
    }

    public record BookingSummary(Long id, String bookingCode, String status, BigDecimal totalPrice,
                                 List<String> seats, LocalDateTime createdAt, CustomerSummary customer,
                                 TripSummary trip) { }
    public record CustomerSummary(UUID id, String fullName, String email, String phoneNumber) { }
    public record TripSummary(Long id, LocalDateTime departureTime, RouteSummary route) { }
    public record RouteSummary(Long id, String departureLocation, String arrivalLocation) { }
    public record StaffSummary(UUID id, String fullName, String staffCode) { }
}
