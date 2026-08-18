package vn.edu.iuh.nhom01_www.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

}
