package vn.edu.iuh.nhom01_www.trip.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.dto.TripRequest;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.impl.TripServiceImpl;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceCompatibilityTest {
    @Mock TripRepository trips;
    @Mock BookingRepository bookings;
    @Mock TripBookingAvailabilityService availability;
    @Mock RouteRepository routes;
    @Mock VehicleRepository vehicles;
    @InjectMocks TripServiceImpl service;

    @Test void createsAndPersistsTripWithDefaultStatusAndLocalTime() {
        var request = request();
        stubReferences();
        when(trips.save(any())).thenAnswer(call -> call.getArgument(0));
        var trip = service.createTrip(request);
        assertEquals("PENDING", trip.getStatus());
        assertEquals(request.getDepartureTime(), trip.getDepartureTime());
        assertEquals(request.getPrice(), trip.getPrice());
        assertEquals(1L, trip.getRoute().getId());
        verify(trips).save(trip);
    }

    @Test void updatesExistingTripInsteadOfReturningNull() {
        var trip = Trip.builder().id(5L).route(Route.builder().id(1L).build()).vehicle(Vehicle.builder().id(2L).build()).build();
        when(trips.findById(5L)).thenReturn(Optional.of(trip));
        stubReferences();
        when(trips.save(any())).thenAnswer(call -> call.getArgument(0));
        var request = request();
        request.setStatus("RUNNING");
        assertSame(trip, service.updateTrip(5L, request));
        assertEquals("RUNNING", trip.getStatus());
    }

    @Test void refusesVehicleChangeWhenBookingsExist() {
        when(trips.findById(5L)).thenReturn(Optional.of(Trip.builder().id(5L)
                .route(Route.builder().id(1L).build()).vehicle(Vehicle.builder().id(3L).build()).build()));
        when(bookings.findByTripId(5L)).thenReturn(List.of(Booking.builder().build()));
        assertThrows(CustomException.class, () -> service.updateTrip(5L, request()));
        verify(trips, never()).save(any());
    }

    @Test void rejectsNegativePrice() {
        var request = request();
        request.setPrice(BigDecimal.valueOf(-1));
        assertThrows(CustomException.class, () -> service.createTrip(request));
        verify(trips, never()).save(any());
    }

    @Test void rejectsInvalidSearchDate() {
        assertThrows(CustomException.class, () -> service.searchTrips("", "", "not-a-date"));
        verifyNoInteractions(trips);
    }

    @Test void searchUsesDatabaseSpecificationAndSorting() {
        when(trips.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        assertTrue(service.searchTrips("TP.HCM", "Đà Lạt", "2026-11-01").isEmpty());
        verify(trips).findAll(any(Specification.class), eq(Sort.by("departureTime")));
        verify(trips, never()).findAll();
    }

    @Test void rejectsUnknownStatus() {
        assertThrows(CustomException.class, () -> service.updateTripStatus(5L, "UNKNOWN"));
        verifyNoInteractions(trips);
    }

    private void stubReferences() {
        when(routes.findById(1L)).thenReturn(Optional.of(Route.builder().id(1L).build()));
        when(vehicles.findById(2L)).thenReturn(Optional.of(Vehicle.builder().id(2L).build()));
    }

    private TripRequest request() {
        var request = new TripRequest();
        request.setRouteId(1L);
        request.setVehicleId(2L);
        request.setDepartureTime(LocalDateTime.of(2026, 11, 1, 10, 30));
        request.setPrice(BigDecimal.valueOf(250000));
        return request;
    }
}
