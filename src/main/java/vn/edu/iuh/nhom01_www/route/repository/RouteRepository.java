package vn.edu.iuh.nhom01_www.route.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByDepartureLocationIgnoreCaseAndArrivalLocationIgnoreCase(String departureLocation, String arrivalLocation);
    List<Route> findByDepartureLocationIgnoreCase(String departureLocation);
    List<Route> findByArrivalLocationIgnoreCase(String arrivalLocation);
}
