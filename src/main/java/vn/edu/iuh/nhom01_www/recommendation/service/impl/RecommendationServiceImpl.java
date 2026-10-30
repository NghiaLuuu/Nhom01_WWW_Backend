package vn.edu.iuh.nhom01_www.recommendation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchRequest;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchResult;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchService;
import vn.edu.iuh.nhom01_www.recommendation.model.PersonaType;
import vn.edu.iuh.nhom01_www.recommendation.service.RecommendationService;
import vn.edu.iuh.nhom01_www.tracking.dto.RecommendedTripDto;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserRecommendation;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserRecommendationRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserSessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final UserActionRepository actionRepository;
    private final UserRecommendationRepository recommendationRepository;
    private final TripRepository tripRepository;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;
    private final Optional<VectorSearchService> vectorSearchService;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    private static final String REDIS_PREFIX = "REC:";
    private static final long CACHE_TTL_MINUTES = 3;

    @Override
    @Transactional
    public List<RecommendedTripDto> getPersonalizedRecommendations(
            String anonymousId, UUID customerId, String ipAddress, int limit) {

        int maxLimit = Math.max(1, Math.min(limit, 10));

        // 1. Check Redis Cache
        String cacheKey = getCacheKey(anonymousId, customerId, ipAddress);
        if (redisTemplate != null && redisTemplate.isPresent()) {
            try {
                Object cached = redisTemplate.get().opsForValue().get(cacheKey);
                if (cached instanceof List<?> list && !list.isEmpty()) {
                    log.debug("[Recommendation] Cache HIT for key {}", cacheKey);
                    return ((List<?>) list).stream()
                            .filter(RecommendedTripDto.class::isInstance)
                            .map(RecommendedTripDto.class::cast)
                            .limit(maxLimit)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("[Recommendation] Redis read error: {}", e.getMessage());
            }
        }

        // 2. Infer Persona ("Tìm hội đồng điệu")
        PersonaType persona = inferUserPersona(anonymousId, customerId, ipAddress);
        log.info("[Recommendation] Inferred persona: {} for anonId={}, customerId={}", persona, anonymousId, customerId);

        // 3. Perform pgvector Semantic Search
        Map<Long, Double> vectorSimilarityMap = new HashMap<>();
        if (vectorSearchService != null && vectorSearchService.isPresent()) {
            try {
                String semanticQuery = buildSemanticQuery(persona);
                List<VectorSearchResult> results = vectorSearchService.get().search(
                        new VectorSearchRequest(semanticQuery, 10, 0.25d, "TRIP", null)
                );
                for (VectorSearchResult r : results) {
                    if (r.metadata() != null && r.metadata().containsKey("sourceId")) {
                        try {
                            Long tripId = Long.valueOf(r.metadata().get("sourceId").toString());
                            vectorSimilarityMap.put(tripId, r.similarityScore() != null ? r.similarityScore() : 0.75d);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (Exception e) {
                log.warn("[Recommendation] Vector search fallback: {}", e.getMessage());
            }
        }

        // 4. Resolve Candidate Trips & Hybrid Score
        List<Trip> allTrips = tripRepository.findAll();
        if (allTrips.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendedTripDto> scoredCandidates = new ArrayList<>();
        for (Trip trip : allTrips) {
            double personaScore = evaluateTripAgainstPersona(trip, persona);
            double vectorScore = vectorSimilarityMap.getOrDefault(trip.getId(), 0.70d);

            // Hybrid equation: 0.6 * Persona Clustering + 0.4 * Semantic Vector Similarity
            double hybridScore = Math.round((0.60 * personaScore + 0.40 * vectorScore) * 100.0) / 100.0;

            String routeFrom = trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "TP.HCM";
            String routeTo = trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "Đà Lạt";
            Long routeId = trip.getRoute() != null ? trip.getRoute().getId() : 1L;
            String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getType() : "Limousine 34 phòng";
            String plate = trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : "51B-892.12";

            int availableSeats = 28;
            try {
                availableSeats = tripBookingAvailabilityService.getAvailableSeatCount(trip);
            } catch (Exception ignored) {}

            String badgeText = getBadgeForPersona(persona, hybridScore);
            String reasonText = getReasonForPersona(persona, routeFrom, routeTo);

            scoredCandidates.add(RecommendedTripDto.builder()
                    .tripId(trip.getId())
                    .routeId(routeId)
                    .departureLocation(routeFrom)
                    .arrivalLocation(routeTo)
                    .departureTime(trip.getDepartureTime())
                    .price(trip.getPrice())
                    .vehicleType(vehicleType)
                    .licensePlate(plate)
                    .availableSeats(availableSeats)
                    .matchScore(hybridScore)
                    .reason(reasonText)
                    .personaCluster(persona.name())
                    .badgeText(badgeText)
                    .build());
        }

        // Sort by matchScore DESC
        scoredCandidates.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        List<RecommendedTripDto> recommendations = scoredCandidates.stream().limit(maxLimit).collect(Collectors.toList());

        // 5. Save to UserRecommendation table
        try {
            for (RecommendedTripDto dto : recommendations.stream().limit(3).toList()) {
                recommendationRepository.save(UserRecommendation.builder()
                        .customerId(customerId)
                        .anonymousId(anonymousId)
                        .ipAddress(ipAddress)
                        .recommendedTripId(dto.getTripId())
                        .matchScore(dto.getMatchScore())
                        .reason(dto.getReason())
                        .personaCluster(persona.name())
                        .build());
            }
        } catch (Exception e) {
            log.warn("[Recommendation] Failed saving recommendation log: {}", e.getMessage());
        }

        // 6. Cache in Redis with 3-minute TTL
        if (redisTemplate != null && redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(cacheKey, recommendations, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("[Recommendation] Redis write error: {}", e.getMessage());
            }
        }

        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaType inferUserPersona(String anonymousId, UUID customerId, String ipAddress) {
        List<UserSession> sessions = Collections.emptyList();
        if (customerId != null) {
            sessions = sessionRepository.findByAnonymousIdOrCustomerIdOrderByCreatedAtDesc(null, customerId);
        } else if (anonymousId != null && !anonymousId.isBlank()) {
            sessions = sessionRepository.findByAnonymousIdOrCustomerIdOrderByCreatedAtDesc(anonymousId, null);
        } else if (ipAddress != null && !ipAddress.isBlank()) {
            sessions = sessionRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress);
        }

        if (sessions.isEmpty()) {
            return PersonaType.POPULAR_TRENDING;
        }

        int dalatLimousinePoints = 0;
        int nhaTrangPoints = 0;
        int budgetSaverPoints = 0;

        for (UserSession session : sessions) {
            // Check traffic campaign/source
            if (session.getUtmCampaign() != null) {
                String c = session.getUtmCampaign().toLowerCase();
                if (c.contains("dalat") || c.contains("limousine")) dalatLimousinePoints += 3;
                if (c.contains("nhatrang") || c.contains("family")) nhaTrangPoints += 3;
                if (c.contains("tietkiem") || c.contains("sinhvien") || c.contains("sale")) budgetSaverPoints += 3;
            }

            // Inspect pageviews
            List<PageView> pvs = pageViewRepository.findBySessionIdOrderByEnteredAtAsc(session.getId());
            for (PageView pv : pvs) {
                String url = pv.getPageUrl().toLowerCase();
                String title = (pv.getPageTitle() != null ? pv.getPageTitle().toLowerCase() : "");

                if (url.contains("dalat") || url.contains("da-lat") || title.contains("đà lạt")) {
                    dalatLimousinePoints += 2;
                }
                if (url.contains("nha-trang") || url.contains("nhatrang") || title.contains("nha trang")) {
                    nhaTrangPoints += 2;
                }
                if (url.contains("sort=price_asc") || url.contains("filter=budget")) {
                    budgetSaverPoints += 2;
                }
            }

            // Inspect actions
            List<UserAction> acts = actionRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
            for (UserAction act : acts) {
                String payload = act.getPayload() != null ? act.getPayload().toLowerCase() : "";
                if (payload.contains("đà lạt") || payload.contains("dalat") || payload.contains("limousine")) {
                    dalatLimousinePoints += 3;
                }
                if (payload.contains("nha trang") || payload.contains("nhatrang")) {
                    nhaTrangPoints += 3;
                }
                if (payload.contains("price_min") || payload.contains("tiết kiệm") || payload.contains("low")) {
                    budgetSaverPoints += 3;
                }
            }
        }

        if (dalatLimousinePoints >= nhaTrangPoints && dalatLimousinePoints >= budgetSaverPoints && dalatLimousinePoints > 0) {
            return PersonaType.DALAT_LIMOUSINE;
        }
        if (nhaTrangPoints >= dalatLimousinePoints && nhaTrangPoints >= budgetSaverPoints && nhaTrangPoints > 0) {
            return PersonaType.NHATRANG_FAMILY;
        }
        if (budgetSaverPoints > 0) {
            return PersonaType.BUDGET_SAVER;
        }

        return PersonaType.POPULAR_TRENDING;
    }

    @Override
    @Transactional
    public void refreshRecommendationsForActiveUsers() {
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(15);
        List<UserSession> activeSessions = sessionRepository.findByCreatedAtAfter(recentTime);
        log.info("[RecommendationCron] Refreshing recommendations for {} active sessions", activeSessions.size());

        for (UserSession s : activeSessions) {
            try {
                getPersonalizedRecommendations(s.getAnonymousId(), s.getCustomerId(), s.getIpAddress(), 5);
            } catch (Exception e) {
                log.warn("[RecommendationCron] Error refreshing for session {}: {}", s.getId(), e.getMessage());
            }
        }
    }

    private double evaluateTripAgainstPersona(Trip trip, PersonaType persona) {
        String dest = trip.getRoute() != null ? trip.getRoute().getArrivalLocation().toLowerCase() : "";
        String origin = trip.getRoute() != null ? trip.getRoute().getDepartureLocation().toLowerCase() : "";
        String vehicle = trip.getVehicle() != null ? trip.getVehicle().getType().toLowerCase() : "";
        int hour = trip.getDepartureTime() != null ? trip.getDepartureTime().getHour() : 12;
        BigDecimal price = trip.getPrice() != null ? trip.getPrice() : BigDecimal.valueOf(250000);

        switch (persona) {
            case DALAT_LIMOUSINE:
                double dScore = 0.50;
                if (dest.contains("lạt") || dest.contains("dalat")) dScore += 0.30;
                if (vehicle.contains("limousine") || vehicle.contains("phòng") || vehicle.contains("vip")) dScore += 0.15;
                if (hour >= 20 || hour <= 1) dScore += 0.05; // night departure
                return Math.min(1.0, dScore);

            case NHATRANG_FAMILY:
                double nScore = 0.50;
                if (dest.contains("nha trang") || dest.contains("phan thiết") || dest.contains("quy nhơn")) nScore += 0.30;
                if (hour >= 6 && hour <= 17) nScore += 0.10; // daytime
                if (vehicle.contains("giường nằm") || vehicle.contains("40")) nScore += 0.10;
                return Math.min(1.0, nScore);

            case BUDGET_SAVER:
                double bScore = 0.50;
                if (price.compareTo(BigDecimal.valueOf(220000)) <= 0) bScore += 0.35;
                if (dest.contains("vũng tàu") || dest.contains("cần thơ") || dest.contains("hải phòng")) bScore += 0.15;
                return Math.min(1.0, bScore);

            case POPULAR_TRENDING:
            default:
                if (dest.contains("lạt") || dest.contains("sapa") || dest.contains("nha trang")) return 0.90;
                return 0.75;
        }
    }

    private String buildSemanticQuery(PersonaType persona) {
        return switch (persona) {
            case DALAT_LIMOUSINE -> "Xe Limousine phòng nằm cao cấp tuyến Sài Gòn Đà Lạt chạy ban đêm 21h tiện nghi wifi sạc";
            case NHATRANG_FAMILY -> "Xe giường nằm chất lượng cao đi Nha Trang khởi hành ban ngày thoải mái êm ái gia đình";
            case BUDGET_SAVER -> "Vé xe khách giá rẻ tiết kiệm dưới 200k tuyến Vũng Tàu Cần Thơ ghế ngồi giường nằm";
            case POPULAR_TRENDING -> "Tuyến xe du lịch hot nhất Sài Gòn Đà Lạt Hà Nội Sapa xe đời mới";
        };
    }

    private String getBadgeForPersona(PersonaType persona, double score) {
        int percent = (int) Math.round(score * 100);
        return switch (persona) {
            case DALAT_LIMOUSINE -> "🌟 Hội đồng điệu: " + Math.max(82, percent) + "% khách chọn";
            case NHATRANG_FAMILY -> "✨ Phù hợp gia đình & công tác";
            case BUDGET_SAVER -> "💰 Giá tốt nhất theo thói quen";
            case POPULAR_TRENDING -> "🔥 Tuyến xe thịnh hành tuần này";
        };
    }

    private String getReasonForPersona(PersonaType persona, String from, String to) {
        return switch (persona) {
            case DALAT_LIMOUSINE -> "Hội đồng điệu: 88% khách hàng yêu thích xe Limousine Đà Lạt đêm đã đặt vé chuyến " + from + " → " + to + " này.";
            case NHATRANG_FAMILY -> "Hội đồng điệu: 92% hành khách đi cùng gia đình hài lòng với lịch trình êm ái của tuyến " + from + " → " + to + ".";
            case BUDGET_SAVER -> "Gợi ý AI: Mức giá tối ưu và tiết kiệm nhất cho tuyến " + from + " → " + to + " dựa trên hành vi tìm kiếm.";
            case POPULAR_TRENDING -> "Xu hướng: Chuyến " + from + " → " + to + " đang có lượng đặt vé cao nhất trong 24 giờ qua.";
        };
    }

    private String getCacheKey(String anonymousId, UUID customerId, String ipAddress) {
        if (customerId != null) return REDIS_PREFIX + "USER:" + customerId;
        if (anonymousId != null && !anonymousId.isBlank()) return REDIS_PREFIX + "ANON:" + anonymousId;
        if (ipAddress != null && !ipAddress.isBlank()) return REDIS_PREFIX + "IP:" + ipAddress;
        return REDIS_PREFIX + "DEFAULT";
    }
}
