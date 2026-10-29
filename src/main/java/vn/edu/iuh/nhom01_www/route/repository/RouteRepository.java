package vn.edu.iuh.nhom01_www.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.route.model.Route;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByStatusTrue();

    @Query("SELECT r FROM Route r WHERE r.status = true " +
           "AND (:departure IS NULL OR :departure = '' OR LOWER(r.departureLocation) LIKE LOWER(CONCAT('%', :departure, '%'))) " +
           "AND (:arrival IS NULL OR :arrival = '' OR LOWER(r.arrivalLocation) LIKE LOWER(CONCAT('%', :arrival, '%')))")
    List<Route> searchRoutes(@Param("departure") String departure, @Param("arrival") String arrival);

    @Query("SELECT DISTINCT r.departureLocation FROM Route r WHERE r.status = true ORDER BY r.departureLocation ASC")
    List<String> findDistinctDepartureLocations();

    @Query("SELECT DISTINCT r.arrivalLocation FROM Route r WHERE r.status = true ORDER BY r.arrivalLocation ASC")
    List<String> findDistinctArrivalLocations();
}
