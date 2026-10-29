package vn.edu.iuh.nhom01_www.analytics.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record TrackEventDto(
        @NotBlank @Size(max = 100) String sessionId,
        @NotBlank @Size(max = 100) String visitorId,
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "PAGE_VIEW|DWELL_TIME|CLICK_EVENT|SEARCH_TRIP|SEARCH_TRIPS|VIEW_SEATS|SELECT_SEAT|FILL_INFO|BOOK_SUCCESS|ABANDON_BOOKING|FUNNEL_STEP|CHATBOT_OPEN|CHATBOT_CLOSE|CHATBOT_QUESTION|CHATBOT_RESPONSE")
        String actionType,
        @Size(max = 2048) String pageUrl,
        @Size(max = 255) String pageTitle,
        @Size(max = 2048) String referrer,
        @Max(86400) Long dwellTimeSeconds,
        Map<String, Object> eventData
) {
}
