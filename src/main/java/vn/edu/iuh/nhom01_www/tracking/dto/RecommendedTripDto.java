package vn.edu.iuh.nhom01_www.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedTripDto {
    private Long tripId;
    private Long routeId;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private BigDecimal price;
    private String vehicleType;
    private String licensePlate;
    private Integer availableSeats;
    private Double matchScore;
    private String reason;
    private String personaCluster;
    private String badgeText; // "Hội đồng điệu lựa chọn" / "Gợi ý AI pgvector"
}

