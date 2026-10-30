package vn.edu.iuh.nhom01_www.tracking.dto;

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
    private int totalSessions;
    private int totalPageViews;
    private int totalActions;
    private String matchedPersona;
    private List<JourneyEventDto> events;

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
