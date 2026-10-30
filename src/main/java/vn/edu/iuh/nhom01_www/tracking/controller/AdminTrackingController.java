package vn.edu.iuh.nhom01_www.tracking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.tracking.dto.CustomerJourneyTimelineDto;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackingAnalyticsResponse;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.service.TrackingService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tracking")
@RequiredArgsConstructor
public class AdminTrackingController {

    private final TrackingService trackingService;

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<TrackingAnalyticsResponse>> getAnalyticsOverview() {
        TrackingAnalyticsResponse response = trackingService.getAnalyticsOverview();
        return ResponseEntity.ok(ApiResponse.success(response, "Analytics fetched successfully"));
    }

    @GetMapping("/journey/{identifier}")
    public ResponseEntity<ApiResponse<CustomerJourneyTimelineDto>> getCustomerJourney(
            @PathVariable String identifier) {

        CustomerJourneyTimelineDto journey = trackingService.getCustomerJourney(identifier);
        return ResponseEntity.ok(ApiResponse.success(journey, "Customer journey fetched"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<UserSession>>> getRecentSessions(
            @RequestParam(defaultValue = "30") int limit) {

        List<UserSession> sessions = trackingService.getRecentSessions(limit);
        return ResponseEntity.ok(ApiResponse.success(sessions, "Recent sessions fetched"));
    }

    @GetMapping("/live-visitors")
    public ResponseEntity<ApiResponse<vn.edu.iuh.nhom01_www.tracking.dto.LiveVisitorsSummaryResponse>> getLiveVisitors(
            @RequestParam(defaultValue = "5") int minutes) {

        vn.edu.iuh.nhom01_www.tracking.dto.LiveVisitorsSummaryResponse response = trackingService.getLiveVisitors(minutes);
        return ResponseEntity.ok(ApiResponse.success(response, "Live visitors fetched successfully"));
    }
}

