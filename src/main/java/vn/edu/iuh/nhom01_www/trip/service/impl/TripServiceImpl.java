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

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;
import vn.edu.iuh.nhom01_www.trip.dto.TripRequest;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;

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
        LocalDate travelDate;
        try {
            travelDate = date == null || date.isBlank() ? null : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new CustomException("date must use yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }
        Specification<Trip> filter = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (departureLocation != null && !departureLocation.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("route").get("departureLocation")), searchPattern(departureLocation), '!'));
            }
            if (arrivalLocation != null && !arrivalLocation.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("route").get("arrivalLocation")), searchPattern(arrivalLocation), '!'));
            }
            if (travelDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("departureTime"), travelDate.atStartOfDay()),
                        cb.lessThan(root.get("departureTime"), travelDate.plusDays(1).atStartOfDay()));
            }
            return predicate;
        };
        List<Trip> trips = tripRepository.findAll(filter, Sort.by("departureTime"));
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
        trip.setAvailableSeats(tripBookingAvailabilityService.getAvailableSeatCount(trip));
        trip.setWebBookingOpen(tripBookingAvailabilityService.isWebBookingOpen(trip));
        trip.setPosBookingOpen(tripBookingAvailabilityService.isPosBookingOpen(trip));
    }


    @Override
    @Transactional
    public Trip createTrip(TripRequest req) {
        Trip trip = Trip.builder().status("PENDING").build();
        applyRequest(trip, req);
        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public Trip updateTrip(Long id, TripRequest req) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));
        // Existing tickets reference seats on the original vehicle and route.
        if ((!trip.getVehicle().getId().equals(req.getVehicleId()) || !trip.getRoute().getId().equals(req.getRouteId()))
                && !bookingRepository.findByTripId(id).isEmpty()) {
            throw new CustomException("Cannot change the vehicle or route of a trip with bookings", HttpStatus.CONFLICT);
        }
        applyRequest(trip, req);
        return tripRepository.save(trip);
    }

    private void applyRequest(Trip trip, TripRequest req) {
        if (req.getPrice() == null || req.getPrice().signum() < 0 || req.getDepartureTime() == null) {
            throw new CustomException("Departure time and a non-negative price are required", HttpStatus.BAD_REQUEST);
        }
        trip.setRoute(routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new CustomException("Route not found", HttpStatus.NOT_FOUND)));
        trip.setVehicle(vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND)));
        trip.setDepartureTime(req.getDepartureTime());
        trip.setPrice(req.getPrice());
        if (req.getStatus() != null) {
            validateStatus(req.getStatus());
            trip.setStatus(req.getStatus());
        }
    }

    private void validateStatus(String status) {
        if (status == null || !java.util.Set.of("PENDING", "RUNNING", "COMPLETED", "CANCELLED").contains(status)) {
            throw new CustomException("Invalid trip status", HttpStatus.BAD_REQUEST);
        }
    }

    private String searchPattern(String value) {
        return "%" + value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    @Override
    public Trip updateTripStatus(Long id, String status) {
        validateStatus(status);
        Trip trip = getTripById(id);
        trip.setStatus(status);
        return tripRepository.save(trip);
    }
}
