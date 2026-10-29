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

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

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
        return tripRepository.findAll();
    }

    @Override
    public List<Trip> searchTrips(String departureLocation, String arrivalLocation, String date) {
        String departure = normalize(departureLocation);
        String arrival = normalize(arrivalLocation);
        String searchDate = normalize(date);

        LocalDateTime from = null;
        LocalDateTime to = null;
        if (searchDate != null) {
            try {
                LocalDate localDate = LocalDate.parse(searchDate);
                from = localDate.atStartOfDay();
                to = localDate.plusDays(1).atStartOfDay();
            } catch (java.time.format.DateTimeParseException exception) {
                throw new CustomException("Date must use yyyy-MM-dd format", HttpStatus.BAD_REQUEST);
            }
        }

        return tripRepository.searchTrips(departure, arrival, from, to);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public Trip getTripById(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));
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
