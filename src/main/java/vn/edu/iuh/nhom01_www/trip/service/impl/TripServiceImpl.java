package vn.edu.iuh.nhom01_www.trip.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripService;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));

        List<Booking> bookings = bookingRepository.findByTripId(id);
        if (bookings != null && !bookings.isEmpty()) {
            throw new CustomException("Cannot delete trip with existing bookings", HttpStatus.CONFLICT);
        }

        tripRepository.delete(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> getAllTrips() {
        List<Trip> trips = tripRepository.findAll();
        trips.forEach(this::populateAvailableSeats);
        return trips;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> searchTrips(String departureLocation, String arrivalLocation, String date) {
        List<Trip> trips;
        if (date != null && !date.trim().isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(date.trim());
                LocalDateTime startTime = localDate.atStartOfDay();
                LocalDateTime endTime = localDate.atTime(LocalTime.MAX);
                trips = tripRepository.searchTrips(departureLocation, arrivalLocation, startTime, endTime);
            } catch (Exception e) {
                trips = tripRepository.searchFutureTrips(departureLocation, arrivalLocation, LocalDateTime.now().minusHours(1));
            }
        } else {
            trips = tripRepository.searchFutureTrips(departureLocation, arrivalLocation, LocalDateTime.now().minusHours(1));
        }
        trips.forEach(this::populateAvailableSeats);
        return trips;
    }

    @Override
    @Transactional(readOnly = true)
    public Trip getTripById(Long id) {
        Trip trip = tripRepository.findTripWithDetailsById(id)
                .orElseGet(() -> tripRepository.findById(id)
                        .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND)));
        populateAvailableSeats(trip);
        return trip;
    }

    private void populateAvailableSeats(Trip trip) {
        if (trip == null) return;
        trip.setAvailableSeats(tripBookingAvailabilityService.getAvailableSeatCount(trip));
        trip.setWebBookingOpen(tripBookingAvailabilityService.isWebBookingOpen(trip));
        trip.setPosBookingOpen(tripBookingAvailabilityService.isPosBookingOpen(trip));
    }

    @Override
    public Trip createTrip(vn.edu.iuh.nhom01_www.trip.dto.TripRequest req) {
        return null; // Stub
    }

    @Override
    public Trip updateTrip(Long id, vn.edu.iuh.nhom01_www.trip.dto.TripRequest req) {
        return null; // Stub
    }

    @Override
    public Trip updateTripStatus(Long id, String status) {
        Trip trip = getTripById(id);
        trip.setStatus(status);
        return tripRepository.save(trip);
    }
}
