package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRoute(Route route);

    List<Trip> findByRoute_Id(Long routeId);

    @Query("SELECT t FROM Trip t " +
           "WHERE t.route.id = :routeId " +
           "AND t.departureTime > :afterTime " +
           "AND t.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> findAvailableNextTripsByRouteId(@Param("routeId") Long routeId, @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "LEFT JOIN FETCH t.vehicle v " +
           "WHERE t.departureTime >= :afterTime " +
           "AND (t.status IS NULL OR t.status != 'CANCELLED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> findUpcomingTrips(@Param("afterTime") LocalDateTime afterTime, Pageable pageable);

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "LEFT JOIN FETCH t.vehicle v " +
           "WHERE LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :dep, '%')) " +
           "AND LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arr, '%')) " +
           "AND t.departureTime >= :startDate " +
           "AND t.departureTime <= :endDate " +
           "AND (t.status IS NULL OR t.status != 'CANCELLED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchByRouteAndDate(
            @Param("dep") String dep,
            @Param("arr") String arr,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "LEFT JOIN FETCH t.vehicle v " +
           "WHERE LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :dep, '%')) " +
           "AND LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arr, '%')) " +
           "AND t.departureTime >= :afterTime " +
           "AND (t.status IS NULL OR t.status != 'CANCELLED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchByRoute(
            @Param("dep") String dep,
            @Param("arr") String arr,
            @Param("afterTime") LocalDateTime afterTime,
            Pageable pageable
    );

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "LEFT JOIN FETCH t.vehicle v " +
           "WHERE LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :dep, '%')) " +
           "AND t.departureTime >= :afterTime " +
           "AND (t.status IS NULL OR t.status != 'CANCELLED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchByDeparture(
            @Param("dep") String dep,
            @Param("afterTime") LocalDateTime afterTime,
            Pageable pageable
    );

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.route r " +
           "LEFT JOIN FETCH t.vehicle v " +
           "WHERE LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arr, '%')) " +
           "AND t.departureTime >= :afterTime " +
           "AND (t.status IS NULL OR t.status != 'CANCELLED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchByArrival(
            @Param("arr") String arr,
            @Param("afterTime") LocalDateTime afterTime,
            Pageable pageable
    );
}
