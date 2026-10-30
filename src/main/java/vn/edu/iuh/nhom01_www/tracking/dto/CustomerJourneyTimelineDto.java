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
public class CustomerJourneyTimelineDto {
    private String identifier;
    private String customerName;
    private String customerEmail;
    
    @JsonProperty("isRegisteredCustomer")
    private boolean isRegisteredCustomer;
    
    private List<String> linkedAnonymousIds;
    private int totalSessions;
    private int totalPageViews;
    private int totalActions;
    private String matchedPersona;
    
    // Flat list of all lifetime events
    private List<JourneyEventDto> events;
    
    // Segmented journeys (Dynamic Intent-Aware Demarcation)
    private List<JourneyClusterDto> journeys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JourneyClusterDto {
        private String journeyId;
        private String title;
        private String status; // CONVERTED, ABANDONED, IN_PROGRESS
        private String targetRoute;
        private String trafficSource;
        private String startTime;
        private String endTime;
        private int durationMinutes;
        private int activeSeconds;
        private int idleSeconds;
        private boolean customerIdentified;
        private List<JourneyEventDto> events;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JourneyEventDto {
        private String id;
        private String type; // SESSION_START, PAGE_VIEW, ACTION
        private String title;
        private String description;
        private String timestamp;
        private Integer activeSeconds;
        private Integer idleSeconds;
        private String metadata;
    }
}
