package vn.edu.iuh.nhom01_www.vehicle.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;
import vn.edu.iuh.nhom01_www.vehicle.service.VehicleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    public Vehicle createVehicle(VehicleRequest req) {
        Vehicle vehicle = Vehicle.builder()
                .licensePlate(req.getLicensePlate())
                .capacity(req.getCapacity())
                .status("ACTIVE")
                .build();
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(Long id, VehicleRequest req) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));
        vehicle.setLicensePlate(req.getLicensePlate());
        vehicle.setCapacity(req.getCapacity());
        return vehicleRepository.save(vehicle);
    }

    @Override
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));
        vehicleRepository.delete(vehicle);
    }
}
