package vn.edu.iuh.nhom01_www.booking.dto;

import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Web presentation contract; never serialize the full customer/entity graph. */
public record WebBookingView(Long id, String bookingCode, TripView trip, CustomerView customer,
                             List<String> seats, BigDecimal totalPrice, String status, LocalDateTime createdAt,
                             Long cancelRequestId, String cancelRequestStatus, String cancelReason,
                             String rejectReason, boolean canCancel) {
    public record CustomerView(UUID id, String fullName, String email, String status) {}
    public record RouteView(Long id, String departureLocation, String arrivalLocation, BigDecimal basePrice) {}
    public record VehicleView(Long id, String licensePlate, Integer capacity) {}
    public record TripView(Long id, RouteView route, VehicleView vehicle, LocalDateTime departureTime,
                           BigDecimal price, String status) {}

    public static WebBookingView from(Booking booking, CancelRequest request) {
        var trip = booking.getTrip();
        var route = trip.getRoute();
        var vehicle = trip.getVehicle();
        var customer = booking.getCustomer();
        var tickets = booking.getTickets() == null ? List.<vn.edu.iuh.nhom01_www.ticket.model.Ticket>of() : booking.getTickets();
        boolean pending = request != null && "PENDING".equals(request.getStatus());
        boolean canCancel = request == null && "PAID".equals(booking.getStatus())
                && trip.getDepartureTime() != null && trip.getDepartureTime().isAfter(LocalDateTime.now())
                && !tickets.isEmpty() && tickets.stream().allMatch(t -> "ISSUED".equals(t.getStatus()));
        return new WebBookingView(booking.getId(), "BKG-" + booking.getId(),
                new TripView(trip.getId(), new RouteView(route.getId(), route.getDepartureLocation(), route.getArrivalLocation(), route.getBasePrice()),
                        new VehicleView(vehicle.getId(), vehicle.getLicensePlate(), vehicle.getCapacity()),
                        trip.getDepartureTime(), trip.getPrice(), trip.getStatus()),
                new CustomerView(customer.getId(), customer.getFullName(), customer.getEmail(), customer.getStatus()),
                tickets.stream().map(t -> t.getSeat().getSeatName()).toList(), booking.getTotalPrice(),
                pending ? "CANCEL_REQUESTED" : booking.getStatus(), booking.getCreatedAt(),
                request == null ? null : request.getId(), request == null ? null : request.getStatus(),
                request == null ? null : request.getReason(), request == null ? null : request.getRejectReason(), canCancel);
    }
}
