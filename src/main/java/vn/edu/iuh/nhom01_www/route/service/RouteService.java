package vn.edu.iuh.nhom01_www.route.service;

import vn.edu.iuh.nhom01_www.route.dto.RouteRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;

import java.util.List;
import java.util.Map;

public interface RouteService {
    List<Route> getAllRoutes();
    List<Route> getActiveRoutes();
    List<Route> searchRoutes(String departure, String arrival);
    Map<String, List<String>> getLocations();
    Route getRouteById(Long id);
    Route createRoute(RouteRequest req);
    Route updateRoute(Long id, RouteRequest req);
    void deleteRoute(Long id);
}
