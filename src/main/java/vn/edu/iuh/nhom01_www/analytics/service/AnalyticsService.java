package vn.edu.iuh.nhom01_www.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.analytics.dto.TrackEventDto;
import vn.edu.iuh.nhom01_www.analytics.model.UserActivityLog;
import vn.edu.iuh.nhom01_www.analytics.repository.UserActivityLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserActivityLogRepository activityLogRepository;

    @Async("analyticsTaskExecutor")
    public void trackBatch(List<TrackEventDto> events, Authentication authentication, String ipAddress,
                           String userAgent) {
        String userEmail = authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : null;
        try {
            List<UserActivityLog> logs = events.stream()
                    .map(event -> toEntity(event, userEmail, ipAddress, userAgent))
                    .toList();
            activityLogRepository.saveAll(logs);
        } catch (RuntimeException exception) {
            log.error("Failed to persist {} analytics events", events.size(), exception);
        }
    }

    public Map<String, Object> overview(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("totalEvents", activityLogRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(range.from(), range.to()));
        result.put("activeVisitors", activityLogRepository.countDistinctVisitors(range.from(), range.to()));
        result.put("daily", activityLogRepository.countDailyOverview(range.from(), range.to()));
        result.put("actions", activityLogRepository.countActions(range.from(), range.to()));
        return result;
    }

    public List<Map<String, Object>> dwellTimeByPage(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        return activityLogRepository.findDwellTimeByPage(range.from(), range.to());
    }

    public List<Map<String, Object>> funnelConversion(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        return activityLogRepository.findFunnelConversion(range.from(), range.to());
    }

    public List<Map<String, Object>> searchTrends(LocalDate from, LocalDate to, int limit) {
        DateRange range = dateRange(from, to);
        return activityLogRepository.findSearchTrends(range.from(), range.to(), limit);
    }

    private UserActivityLog toEntity(TrackEventDto event, String userEmail, String ipAddress, String userAgent) {
        return UserActivityLog.builder()
                .sessionId(event.sessionId())
                .visitorId(event.visitorId())
                .userEmail(userEmail)
                .actionType(event.actionType())
                .pageUrl(event.pageUrl())
                .pageTitle(event.pageTitle())
                .referrer(event.referrer())
                .dwellTimeSeconds(event.dwellTimeSeconds())
                .eventData(event.eventData())
                .ipAddress(ipAddress)
                .deviceInfo(userAgent)
                .build();
    }

    private DateRange dateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must be on or after 'from'");
        }
        if (to.isAfter(from.plusDays(366))) {
            throw new IllegalArgumentException("Analytics date range cannot exceed 366 days");
        }
        return new DateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}
