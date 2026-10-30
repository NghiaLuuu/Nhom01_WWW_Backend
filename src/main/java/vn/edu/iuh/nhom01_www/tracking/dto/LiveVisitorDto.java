package vn.edu.iuh.nhom01_www.tracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveVisitorDto {
    private String sessionId;
    private String anonymousId;
    private String customerId;
    
    @JsonProperty("isRegisteredCustomer")
    private boolean isRegisteredCustomer;
    
    private String customerName;
    private String customerEmail;
    private String ipAddress;
    private String userAgent;
    private String deviceType; // Desktop / Mobile
    private String trafficSource;
    private String utmCampaign;
    
    // Current Page & Activity
    private String currentPageUrl;
    private String currentPageTitle;
    private int activeSecondsOnPage;
    private int idleSecondsOnPage;
    private String lastActivityTime;
    private String lastActionType;
    private String lastActionPayload;
    
    // Live AI Recommendations
    private String matchedPersona;
    private List<LiveRecommendedTripDto> recommendedTrips;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LiveRecommendedTripDto {
        private Long tripId;
        private String route;
        private String departureTime;
        private String price;
        private String vehicleType;
    }
}

