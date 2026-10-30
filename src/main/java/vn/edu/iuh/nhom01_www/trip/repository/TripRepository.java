package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "JOIN FETCH t.vehicle v " +
           "WHERE t.id = :id")
    Optional<Trip> findTripWithDetailsById(@Param("id") Long id);

    @Query("SELECT t FROM Trip t " +
           "WHERE t.route.id = :routeId " +
           "AND t.departureTime > :afterTime " +
           "AND t.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> findAvailableNextTripsByRouteId(@Param("routeId") Long routeId, @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "JOIN FETCH t.vehicle v " +
           "WHERE (:departure IS NULL OR :departure = '' OR LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :departure, '%'))) " +
           "AND (:arrival IS NULL OR :arrival = '' OR LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arrival, '%'))) " +
           "AND t.departureTime >= :startTime AND t.departureTime <= :endTime " +
           "AND t.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchTrips(
            @Param("departure") String departure,
            @Param("arrival") String arrival,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "JOIN FETCH t.vehicle v " +
           "WHERE (:departure IS NULL OR :departure = '' OR LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :departure, '%'))) " +
           "AND (:arrival IS NULL OR :arrival = '' OR LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arrival, '%'))) " +
           "AND t.departureTime >= :now " +
           "AND t.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchFutureTrips(
            @Param("departure") String departure,
            @Param("arrival") String arrival,
            @Param("now") LocalDateTime now
    );
}
