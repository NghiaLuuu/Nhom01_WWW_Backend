package vn.edu.iuh.nhom01_www.aivector;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VectorSearchRequest(
        @NotBlank @Size(max = 1_000) String query,
        @Min(1) @Max(20) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold,
        @Pattern(regexp = "(?i)ROUTE|TRIP|VEHICLE", message = "sourceType must be ROUTE, TRIP, or VEHICLE") String sourceType,
        @Positive Long routeId) {
}
