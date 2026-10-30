package vn.edu.iuh.nhom01_www.recommendation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.recommendation.model.PersonaType;
import vn.edu.iuh.nhom01_www.recommendation.seeder.TrackingBehaviorSeeder;
import vn.edu.iuh.nhom01_www.recommendation.service.RecommendationService;
import vn.edu.iuh.nhom01_www.tracking.dto.RecommendedTripDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final TrackingBehaviorSeeder seeder;

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) {
            return xr.trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendedTripDto>>> getRecommendations(
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        List<RecommendedTripDto> list = recommendationService.getPersonalizedRecommendations(anonymousId, customerId, ip, limit);
        return ResponseEntity.ok(ApiResponse.success(list, "Personalized recommendations retrieved"));
    }

    @GetMapping("/persona")
    public ResponseEntity<ApiResponse<Map<String, String>>> inferPersona(
            @RequestParam(required = false) String anonymousId,
            @RequestParam(required = false) UUID customerId,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        PersonaType persona = recommendationService.inferUserPersona(anonymousId, customerId, ip);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "persona", persona.name(),
                "displayName", persona.getDisplayName(),
                "description", persona.getDescription()
        ), "Inferred user persona cluster"));
    }

    @PostMapping("/reseed")
    public ResponseEntity<ApiResponse<String>> reseedData() {
        seeder.seedAllPersonas();
        return ResponseEntity.ok(ApiResponse.success("Behavior tracking seed data inserted successfully!"));
    }
}

