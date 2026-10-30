package vn.edu.iuh.nhom01_www.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingAnalyticsResponse {
    private long totalSessions;
    private long uniqueVisitors;
    private double avgSessionDurationSeconds;
    private Map<String, Long> trafficSources;
    private Map<String, Long> actionBreakdown;
    private List<PageStatDto> topPages;
    private List<RecentSessionDto> recentSessions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageStatDto {
        private String pageUrl;
        private long viewCount;
        private double avgActiveSeconds;
        private double avgIdleSeconds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentSessionDto {
        private String sessionId;
        private String anonymousId;
        private String customerId;
        private String ipAddress;
        private String trafficSource;
        private Integer totalDurationSeconds;
        private String createdAt;
        private int pageViewCount;
        private int actionCount;
    }
}
