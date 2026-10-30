package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Trip> {

    @Query("SELECT t FROM Trip t " +
           "WHERE t.route.id = :routeId " +
           "AND t.departureTime > :afterTime " +
           "AND t.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "ORDER BY t.departureTime ASC")
    List<Trip> findAvailableNextTripsByRouteId(@Param("routeId") Long routeId, @Param("afterTime") LocalDateTime afterTime);
}
