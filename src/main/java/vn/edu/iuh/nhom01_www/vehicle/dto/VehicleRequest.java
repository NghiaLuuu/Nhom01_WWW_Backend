package vn.edu.iuh.nhom01_www.vehicle.dto;

import lombok.Data;

@Data
public class VehicleRequest {
    @jakarta.validation.constraints.NotBlank
    private String licensePlate;
    private Integer capacity;
}
