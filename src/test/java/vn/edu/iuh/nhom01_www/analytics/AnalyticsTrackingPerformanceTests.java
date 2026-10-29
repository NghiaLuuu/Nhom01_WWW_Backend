package vn.edu.iuh.nhom01_www.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.iuh.nhom01_www.analytics.service.AnalyticsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsTrackingPerformanceTests {

    private static final String ENDPOINT = "/api/analytics/events/batch";
    private static final int P95_LIMIT_MILLIS = 50;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void rejectsInvalidBatchAndEventPayloads() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"events":[{"sessionId":"","visitorId":"visitor-1","actionType":"INVALID"}]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("events", List.of()))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("events", events(101)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsBatchContainingOneHundredEvents() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("events", events(100)))))
                .andExpect(status().isAccepted());

        verify(analyticsService).trackBatch(
                org.mockito.ArgumentMatchers.argThat(batch -> batch.size() == 100),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class));
    }

    @Test
    void measuresConcurrentBatchLatencyAndRequiresP95BelowFiftyMilliseconds() throws Exception {
        for (int i = 0; i < 10; i++) {
            sendBatch();
        }

        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Long>> requests = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(this::sendBatch));
            }

            List<Long> durations = new ArrayList<>();
            for (Future<Long> request : requests) {
                durations.add(request.get(10, TimeUnit.SECONDS));
            }

            Collections.sort(durations);
            long p50 = percentile(durations, 0.50);
            long p95 = percentile(durations, 0.95);
            long p99 = percentile(durations, 0.99);

            System.out.printf(
                    "Analytics batch latency: p50=%dms, p95=%dms, p99=%dms%n",
                    p50, p95, p99);
            assertTrue(p95 < P95_LIMIT_MILLIS,
                    "Analytics batch p95 must be below " + P95_LIMIT_MILLIS + "ms, actual: " + p95 + "ms");
        } finally {
            executor.shutdownNow();
        }
    }

    private long sendBatch() throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("events", events(100)))))
                .andExpect(status().isAccepted());
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private List<Map<String, Object>> events(int count) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(Map.of(
                    "sessionId", "session-" + i,
                    "visitorId", "visitor-" + i,
                    "actionType", "PAGE_VIEW",
                    "pageUrl", "/home"
            ));
        }
        return events;
    }

    private long percentile(List<Long> sortedValues, double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
}
