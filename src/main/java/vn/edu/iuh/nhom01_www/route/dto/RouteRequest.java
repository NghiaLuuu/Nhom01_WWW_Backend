package vn.edu.iuh.nhom01_www.route.dto;

import lombok.Data;

@Data
public class RouteRequest {
    @jakarta.validation.constraints.NotBlank
    private String departureLocation;
    @jakarta.validation.constraints.NotBlank
    private String arrivalLocation;
    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal basePrice;
}
