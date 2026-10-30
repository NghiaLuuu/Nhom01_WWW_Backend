package vn.edu.iuh.nhom01_www.trip.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Determines whether a trip can still accept a booking without changing its permanent status. */
@Service
@RequiredArgsConstructor
public class TripBookingAvailabilityService {

    private final TicketRepository ticketRepository;
    private final SeatHoldRedisService seatHoldRedisService;

    @Value("${booking.cutoff.web-minutes-before:30}")
    private int configuredWebCloseMinutes;

    @Value("${booking.cutoff.pos-minutes-before:10}")
    private int configuredPosCloseMinutes;

    public int getAvailableSeatCount(Trip trip) {
        if (trip == null || trip.getId() == null) return 0;
        int capacity = trip.getVehicle() != null && trip.getVehicle().getCapacity() != null
                ? trip.getVehicle().getCapacity() : 45;

        Set<Long> unavailableSeatIds = new HashSet<>(ticketRepository.findBookedSeatIdsByTripId(trip.getId()));
        unavailableSeatIds.addAll(seatHoldRedisService.getHeldSeatIdsByTripId(trip.getId()));
        return Math.max(0, capacity - unavailableSeatIds.size());
    }

    public boolean isWebBookingOpen(Trip trip) {
        return isBookingOpen(trip, effectiveWebCloseMinutes());
    }

    public boolean isPosBookingOpen(Trip trip) {
        return isBookingOpen(trip, effectivePosCloseMinutes());
    }

    public void ensureWebBookingOpen(Trip trip) {
        ensureBookingOpen(trip, effectiveWebCloseMinutes(), "trên website");
    }

    public void ensurePosBookingOpen(Trip trip) {
        ensureBookingOpen(trip, effectivePosCloseMinutes(), "tại quầy");
    }

    private boolean isBookingOpen(Trip trip, int closeMinutesBefore) {
        if (trip == null || trip.getDepartureTime() == null || isUnavailableStatus(trip)) return false;
        if (!LocalDateTime.now().isBefore(trip.getDepartureTime().minusMinutes(closeMinutesBefore))) return false;
        return getAvailableSeatCount(trip) > 0;
    }

    private void ensureBookingOpen(Trip trip, int closeMinutesBefore, String channel) {
        if (trip == null || isUnavailableStatus(trip)) {
            throw new CustomException("Chuyến xe này không còn mở bán vé!", HttpStatus.CONFLICT);
        }
        if (trip.getDepartureTime() == null || !LocalDateTime.now().isBefore(trip.getDepartureTime().minusMinutes(closeMinutesBefore))) {
            throw new CustomException(
                    "Đã đóng cổng bán vé " + channel + " trước giờ khởi hành " + closeMinutesBefore + " phút!",
                    HttpStatus.CONFLICT);
        }
        if (getAvailableSeatCount(trip) <= 0) {
            throw new CustomException("Chuyến xe đã đủ 100% số ghế và tạm đóng bán vé!", HttpStatus.CONFLICT);
        }
    }

    // The configured values are intentionally constrained to the business ranges.
    private int effectiveWebCloseMinutes() {
        return Math.max(30, Math.min(configuredWebCloseMinutes, 60));
    }

    private int effectivePosCloseMinutes() {
        return Math.max(10, Math.min(configuredPosCloseMinutes, 15));
    }

    private boolean isUnavailableStatus(Trip trip) {
        return "CANCELLED".equalsIgnoreCase(trip.getStatus())
                || "COMPLETED".equalsIgnoreCase(trip.getStatus());
    }
}
