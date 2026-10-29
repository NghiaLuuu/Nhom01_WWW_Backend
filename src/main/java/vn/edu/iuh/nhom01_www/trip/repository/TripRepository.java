package vn.edu.iuh.nhom01_www.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query(value = """
            select distinct t.*
            from trips t
            join routes r on r.id = t.route_id
            join vehicles v on v.id = t.vehicle_id
            where (cast(:departure as text) is null
                   or lower(r.departure_location) = lower(cast(:departure as text)))
              and (cast(:arrival as text) is null
                   or lower(r.arrival_location) = lower(cast(:arrival as text)))
              and (cast(:fromTime as timestamp) is null
                   or t.departure_time >= cast(:fromTime as timestamp))
              and (cast(:toTime as timestamp) is null
                   or t.departure_time < cast(:toTime as timestamp))
              and r.status = true
            order by t.departure_time
            """, nativeQuery = true)
    List<Trip> searchTrips(@Param("departure") String departure,
                           @Param("arrival") String arrival,
                           @Param("fromTime") LocalDateTime from,
                           @Param("toTime") LocalDateTime to);
}
