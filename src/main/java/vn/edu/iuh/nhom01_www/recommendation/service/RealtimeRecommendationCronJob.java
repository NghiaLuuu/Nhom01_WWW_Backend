package vn.edu.iuh.nhom01_www.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeRecommendationCronJob {

    private final RecommendationService recommendationService;

    @Scheduled(fixedRate = 30000) // Runs every 30 seconds
    public void executeRecommendationSync() {
        try {
            recommendationService.refreshRecommendationsForActiveUsers();
        } catch (Exception e) {
            log.warn("[RecommendationCron] Execution exception: {}", e.getMessage());
        }
    }
}
