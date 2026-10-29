package vn.edu.iuh.nhom01_www.analytics.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.nhom01_www.analytics.dto.BatchTrackEventRequest;
import vn.edu.iuh.nhom01_www.analytics.service.AnalyticsService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/analytics/events")
@RequiredArgsConstructor
public class AnalyticsTrackingController {

    private final AnalyticsService analyticsService;

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> trackBatch(@Valid @RequestBody BatchTrackEventRequest request,
                                                        Authentication authentication,
                                                        HttpServletRequest httpRequest) {
        analyticsService.trackBatch(request.events(), authentication,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.accepted().body(new ApiResponse<>(true, "Events accepted", null));
    }
}
