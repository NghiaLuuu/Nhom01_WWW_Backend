package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CancelRequestResponse(Long id, Long bookingId, UUID customerId, String customerName,
                                    Long tripId, LocalDateTime departureTime, BigDecimal bookingAmount,
                                    String status, String reason, String rejectReason,
                                    UUID resolvedBy, String resolvedByName, LocalDateTime createdAt,
                                    LocalDateTime resolvedAt, List<TicketSummary> tickets,
                                    String customerPhone, String departureLocation, String arrivalLocation,
                                    String licensePlate, String bookingStatus, boolean canApprove) {
    public record TicketSummary(Long id, String ticketCode, String status, String seatName, BigDecimal price) {}

    public static CancelRequestResponse from(CancelRequest r) {
        var b = r.getBooking();
        String reason = r.getReason();
        if (reason == null && !"REJECTED".equals(r.getStatus())) reason = r.getRejectReason();
        return new CancelRequestResponse(r.getId(), b.getId(), b.getCustomer().getId(), b.getCustomer().getFullName(),
                b.getTrip().getId(), b.getTrip().getDepartureTime(), b.getTotalPrice(), r.getStatus(), reason,
                "REJECTED".equals(r.getStatus()) ? r.getRejectReason() : null,
                r.getStaff() == null ? null : r.getStaff().getId(),
                r.getStaff() == null ? null : r.getStaff().getFullName(), r.getCreatedAt(), r.getResolvedAt(),
                b.getTickets() == null ? List.of() : b.getTickets().stream()
                        .map(t -> new TicketSummary(t.getId(), t.getTicketCode(), t.getStatus(),
                                t.getSeat() == null ? null : t.getSeat().getSeatName(), t.getPrice())).toList(),
                b.getCustomer().getPhoneNumber(),
                b.getTrip().getRoute() == null ? null : b.getTrip().getRoute().getDepartureLocation(),
                b.getTrip().getRoute() == null ? null : b.getTrip().getRoute().getArrivalLocation(),
                b.getTrip().getVehicle() == null ? null : b.getTrip().getVehicle().getLicensePlate(),
                b.getStatus(), "PENDING".equals(r.getStatus()) && "PAID".equals(b.getStatus())
                        && b.getTrip().getDepartureTime() != null && b.getTrip().getDepartureTime().isAfter(LocalDateTime.now())
                        && b.getTickets() != null && !b.getTickets().isEmpty()
                        && b.getTickets().stream().allMatch(t -> "ISSUED".equals(t.getStatus())));
    }
}
