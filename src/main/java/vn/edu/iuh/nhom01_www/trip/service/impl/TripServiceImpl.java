package vn.edu.iuh.nhom01_www.trip.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;
import vn.edu.iuh.nhom01_www.trip.service.TripService;

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
    public List<Trip> getAllTrips() {
        List<Trip> trips = tripRepository.findAll();
        trips.forEach(this::populateAvailableSeats);
        return trips;
    }

    @Override
    public List<Trip> searchTrips(String departureLocation, String arrivalLocation, String date) {
        String dep = (departureLocation != null && !departureLocation.trim().isEmpty()) ? departureLocation.trim() : null;
        String arr = (arrivalLocation != null && !arrivalLocation.trim().isEmpty()) ? arrivalLocation.trim() : null;
        Pageable pageable = PageRequest.of(0, 100);
        LocalDateTime nowMinus15 = LocalDateTime.now().minusMinutes(15);

        // Case 1: Both departure and arrival provided
        if (dep != null && arr != null) {
            if (date != null && !date.trim().isEmpty()) {
                try {
                    LocalDate parsedDate = LocalDate.parse(date.trim());
                    LocalDateTime start = parsedDate.atStartOfDay();
                    LocalDateTime end = parsedDate.atTime(LocalTime.MAX);
                    List<Trip> trips = tripRepository.searchByRouteAndDate(dep, arr, start, end, pageable);
                    if (!trips.isEmpty()) {
                        trips.forEach(this::populateAvailableSeats);
                        return trips;
                    }
                } catch (Exception ignored) {}
            }
            List<Trip> trips = tripRepository.searchByRoute(dep, arr, nowMinus15, pageable);
            trips.forEach(this::populateAvailableSeats);
            return trips;
        }

        // Case 2: Only departure provided
        if (dep != null) {
            List<Trip> trips = tripRepository.searchByDeparture(dep, nowMinus15, pageable);
            trips.forEach(this::populateAvailableSeats);
            return trips;
        }

        // Case 3: Only arrival provided
        if (arr != null) {
            List<Trip> trips = tripRepository.searchByArrival(arr, nowMinus15, pageable);
            trips.forEach(this::populateAvailableSeats);
            return trips;
        }

        // Case 4: No filters provided (Search page default view: show upcoming trips)
        List<Trip> trips = tripRepository.findUpcomingTrips(nowMinus15, pageable);
        trips.forEach(this::populateAvailableSeats);
        return trips;
    }

    @Override
    public Trip getTripById(Long id) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));
        populateAvailableSeats(trip);
        return trip;
    }

    private void populateAvailableSeats(Trip trip) {
        if (trip == null) return;
        int available = tripBookingAvailabilityService.getAvailableSeatCount(trip);
        trip.setAvailableSeats(available);
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
