package vn.edu.iuh.nhom01_www.route.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.route.dto.RouteRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.service.RouteService;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Route>>> getAllRoutes() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", routeService.getAllRoutes()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Route>> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", routeService.getRouteById(id)));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<Route>> createRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Route created successfully", routeService.createRoute(request)));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<Route>> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Route updated successfully", routeService.updateRoute(id, request)));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route deleted successfully", null));
    }
}
