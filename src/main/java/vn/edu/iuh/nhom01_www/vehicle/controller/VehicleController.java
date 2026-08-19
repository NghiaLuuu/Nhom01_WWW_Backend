package vn.edu.iuh.nhom01_www.vehicle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.service.VehicleService;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VEHICLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getAllVehicles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", vehicleService.getAllVehicles()));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VEHICLE_MANAGE')")
    public ResponseEntity<ApiResponse<Vehicle>> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle created successfully", vehicleService.createVehicle(request)));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VEHICLE_MANAGE')")
    public ResponseEntity<ApiResponse<Vehicle>> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle updated successfully", vehicleService.updateVehicle(id, request)));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VEHICLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle deleted successfully", null));
    }
}
