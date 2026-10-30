package vn.edu.iuh.nhom01_www.tracking.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackActionRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackHeartbeatRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackPageViewRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackSessionRequest;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.service.TrackingService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

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

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrCreateSession(
            @RequestBody(required = false) TrackSessionRequest req,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        UserSession session = trackingService.getOrCreateSession(req, ip, ua);

        Map<String, Object> data = Map.of(
                "sessionId", session.getId(),
                "anonymousId", session.getAnonymousId(),
                "ipAddress", session.getIpAddress() != null ? session.getIpAddress() : "",
                "trafficSource", session.getTrafficSource() != null ? session.getTrafficSource() : "direct"
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Session tracked successfully"));
    }

    @PostMapping("/pageview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordPageView(
            @RequestBody TrackPageViewRequest req) {

        PageView pv = trackingService.recordPageView(req);
        Map<String, Object> data = Map.of(
                "pageViewId", pv.getId(),
                "pageUrl", pv.getPageUrl()
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Pageview recorded"));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @RequestBody TrackHeartbeatRequest req) {

        trackingService.updateHeartbeat(req);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Heartbeat updated"));
    }

    @PostMapping("/action")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordAction(
            @RequestBody TrackActionRequest req) {

        UserAction action = trackingService.recordAction(req);
        Map<String, Object> data = Map.of(
                "actionId", action.getId(),
                "actionType", action.getActionType()
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Action recorded"));
    }

    @PostMapping("/merge-customer")
    public ResponseEntity<ApiResponse<Void>> mergeCustomer(
            @RequestBody Map<String, String> payload) {

        if (payload != null && payload.containsKey("sessionId") && payload.containsKey("customerId")) {
            UUID sessionId = UUID.fromString(payload.get("sessionId"));
            UUID customerId = UUID.fromString(payload.get("customerId"));
            trackingService.mergeCustomerSession(sessionId, customerId);
        }
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Customer session merged"));
    }
}
