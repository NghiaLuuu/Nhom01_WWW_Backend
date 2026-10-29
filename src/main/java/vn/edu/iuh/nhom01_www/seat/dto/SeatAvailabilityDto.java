package vn.edu.iuh.nhom01_www.seat.dto;

public record SeatAvailabilityDto(
        Long seatId,
        String id,
        String seatName,
        Integer floor,
        boolean isBooked,
        boolean isHeld
) {
}
