package vn.edu.iuh.nhom01_www.vehicle.service;

public interface VehicleService {
    java.util.List<vn.edu.iuh.nhom01_www.vehicle.model.Vehicle> getAllVehicles();
    vn.edu.iuh.nhom01_www.vehicle.model.Vehicle createVehicle(vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest req);
    vn.edu.iuh.nhom01_www.vehicle.model.Vehicle updateVehicle(Long id, vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest req);
    void deleteVehicle(Long id);
}
