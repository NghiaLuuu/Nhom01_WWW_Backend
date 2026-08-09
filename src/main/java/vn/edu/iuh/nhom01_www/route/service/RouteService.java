package vn.edu.iuh.nhom01_www.route.service;

public interface RouteService {
    java.util.List<vn.edu.iuh.nhom01_www.route.model.Route> getAllRoutes();
    vn.edu.iuh.nhom01_www.route.model.Route getRouteById(Long id);
    vn.edu.iuh.nhom01_www.route.model.Route createRoute(vn.edu.iuh.nhom01_www.route.dto.RouteRequest req);
    vn.edu.iuh.nhom01_www.route.model.Route updateRoute(Long id, vn.edu.iuh.nhom01_www.route.dto.RouteRequest req);
    void deleteRoute(Long id);
}
