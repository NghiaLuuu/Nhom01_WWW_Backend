package vn.edu.iuh.nhom01_www.trip.service;

import java.util.List;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.dto.TripRequest;

public interface TripService {
    List<Trip> searchTrips(String departureLocation, String arrivalLocation, String date);
    Trip getTripById(Long id);
    Trip createTrip(TripRequest req);
    Trip updateTrip(Long id, TripRequest req);
    Trip updateTripStatus(Long id, String status);
    void deleteTrip(Long id);
}
