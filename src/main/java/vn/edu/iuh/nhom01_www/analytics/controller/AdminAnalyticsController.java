package vn.edu.iuh.nhom01_www.analytics.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.nhom01_www.analytics.service.AnalyticsService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VIEW_STATISTICS')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", analyticsService.overview(from, to)));
    }

    @GetMapping("/dwell-time-by-page")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> dwellTimeByPage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.dwellTimeByPage(from, to)));
    }

    @GetMapping("/funnel-conversion")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> funnelConversion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.funnelConversion(from, to)));
    }

    @GetMapping("/search-trends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.searchTrends(from, to, limit)));
    }

    @GetMapping("/tab-flows")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tabFlows(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.tabFlows(from, to, limit)));
    }

    @GetMapping("/active-users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> activeUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.activeUsers(from, to, limit)));
    }

    @GetMapping("/user-journey")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> userJourney(
            @RequestParam(required = false) String identifier,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success",
                analyticsService.userJourney(identifier, limit)));
    }
}
