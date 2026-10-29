package vn.edu.iuh.nhom01_www.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.route.dto.RouteRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.route.service.RouteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;

    @Override
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    @Override
    public List<Route> getActiveRoutes() {
        return routeRepository.findByStatusTrue();
    }

    @Override
    public List<Route> searchRoutes(String departure, String arrival) {
        return routeRepository.searchRoutes(departure, arrival);
    }

    @Override
    public Map<String, List<String>> getLocations() {
        Map<String, List<String>> locations = new HashMap<>();
        locations.put("departures", routeRepository.findDistinctDepartureLocations());
        locations.put("arrivals", routeRepository.findDistinctArrivalLocations());
        return locations;
    }

    @Override
    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new CustomException("Route not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public Route createRoute(RouteRequest req) {
        Route route = Route.builder()
                .departureLocation(req.getDepartureLocation())
                .arrivalLocation(req.getArrivalLocation())
                .basePrice(req.getBasePrice())
                .status(true)
                .build();
        return routeRepository.save(route);
    }

    @Override
    public Route updateRoute(Long id, RouteRequest req) {
        Route route = getRouteById(id);
        route.setDepartureLocation(req.getDepartureLocation());
        route.setArrivalLocation(req.getArrivalLocation());
        route.setBasePrice(req.getBasePrice());
        return routeRepository.save(route);
    }

    @Override
    public void deleteRoute(Long id) {
        Route route = getRouteById(id);
        routeRepository.delete(route);
    }
}
