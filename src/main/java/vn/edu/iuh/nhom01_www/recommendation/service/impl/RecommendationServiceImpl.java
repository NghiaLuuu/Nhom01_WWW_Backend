package vn.edu.iuh.nhom01_www.recommendation.service.impl;

import org.springframework.data.domain.PageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchRequest;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchResult;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchService;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
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
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;
    private final Optional<VectorSearchService> vectorSearchService;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    private static final String REDIS_PREFIX = "REC:v7:";
    private static final long CACHE_TTL_MINUTES = 3;
    private static final Set<String> METRO_HUBS = Set.of("TP.HCM", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng");

    public record CommuterHabit(String cityA, String cityB, int bookingCount, String dominantDeparture) {}
    public record TopDestination(String cityName, int visitCount, int rank) {}
    public record MultiDestinationHabit(
            String primaryHomeBase,
            List<TopDestination> topDestinations,
            int totalBookings,
            int distinctDestinationsCount
    ) {}

    @Override
    @Transactional
    public List<RecommendedTripDto> getPersonalizedRecommendations(
            String anonymousId, UUID customerId, String ipAddress, String currentCity, int limit) {

        int maxLimit = Math.max(1, Math.min(limit, 10));

        // 1. Check Redis Cache
        String cacheKey = getCacheKey(anonymousId, customerId, ipAddress, currentCity);
        if (redisTemplate != null && redisTemplate.isPresent()) {
            try {
                Object cached = redisTemplate.get().opsForValue().get(cacheKey);
                if (cached instanceof List<?> list && !list.isEmpty()) {
                    log.debug("[Recommendation] Cache HIT for key {}", cacheKey);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    List<RecommendedTripDto> converted = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof RecommendedTripDto dto) {
                            converted.add(dto);
                        } else {
                            try {
                                converted.add(mapper.convertValue(item, RecommendedTripDto.class));
                            } catch (Exception ignored) {}
                        }
                    }
                    if (!converted.isEmpty()) {
                        return converted.stream().limit(maxLimit).collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                log.warn("[Recommendation] Redis read error: {}", e.getMessage());
            }
        }

        // 2. Multi-Destination & Commuter Habit Analysis & Persona Inference
        Optional<MultiDestinationHabit> multiDestOpt = customerId != null ? detectMultiDestinationHabit(customerId) : Optional.empty();
        Optional<CommuterHabit> habitOpt = multiDestOpt.isEmpty() ? detectCommuterHabit(customerId, anonymousId) : Optional.empty();

        PersonaType persona = multiDestOpt.isPresent()
                ? PersonaType.EXPLORER_MULTI_DESTINATION
                : (habitOpt.isPresent()
                        ? PersonaType.COMMUTER_HABIT
                        : inferUserPersona(anonymousId, customerId, ipAddress));

        log.info("[Recommendation] Customer={}, AnonId={}, Inferred Persona={}, CurrentCity={}, HabitPresent={}",
                customerId, anonymousId, persona, currentCity, habitOpt.isPresent());

        // 3. Perform pgvector Semantic Search
        Map<Long, Double> vectorSimilarityMap = new HashMap<>();
        if (vectorSearchService != null && vectorSearchService.isPresent()) {
            try {
                String semanticQuery = buildSemanticQuery(persona, habitOpt.orElse(null));
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

        // 4. Resolve Candidate Trips & Hybrid Score (optimized: prefer upcoming trips, fallback to findAll for compatibility)
        List<Trip> allTrips = Collections.emptyList();
        try {
            allTrips = tripRepository.findUpcomingTrips(LocalDateTime.now().minusMinutes(15), PageRequest.of(0, 300));
        } catch (Exception ignored) {}
        if (allTrips == null || allTrips.isEmpty()) {
            allTrips = tripRepository.findAll();
        }
        if (allTrips.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendedTripDto> scoredCandidates = new ArrayList<>();

        if (habitOpt.isPresent()) {
            // =========================================================================
            // DUAL-ANCHOR COMMUTER HABIT RECOMMENDATION ENGINE
            // =========================================================================
            CommuterHabit habit = habitOpt.get();
            String cityA = habit.cityA(); // Big city hub (e.g. TP.HCM)
            String cityB = habit.cityB(); // Hometown / Province (e.g. Phú Yên)
            boolean hasLocation = currentCity != null && !currentCity.isBlank();

            for (Trip trip : allTrips) {
                String routeFrom = trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "";
                String routeTo = trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "";

                boolean isAtoB = matchesCity(routeFrom, cityA) && matchesCity(routeTo, cityB);
                boolean isBtoA = matchesCity(routeFrom, cityB) && matchesCity(routeTo, cityA);
                boolean isOnCorridor = isAtoB || isBtoA;

                double score;
                String badgeText;
                String reasonText;

                if (!hasLocation) {
                    // Scenario 1: Location OFF -> Recommend both directions on user's corridor
                    if (isOnCorridor) {
                        score = 0.95;
                        badgeText = "Tuyến đường quen thuộc 🔄";
                        reasonText = "Dựa trên thói quen thường xuyên di chuyển giữa " + cityA + " và " + cityB + " của bạn.";
                    } else {
                        score = 0.55;
                        badgeText = "🔥 Tuyến xe thịnh hành tuần này";
                        reasonText = "Chuyến xe phổ biến: " + routeFrom + " → " + routeTo;
                    }
                } else if (matchesCity(currentCity, cityA)) {
                    // Scenario 2: User is at Base A (e.g. TP.HCM) -> Infer going to Base B (Phú Yên)
                    if (isAtoB) {
                        score = 0.98; // TOP #1
                        badgeText = "Về quê / Thăm nhà 🏡";
                        reasonText = "Bạn đang ở " + currentCity + " - Ưu tiên các chuyến xe về " + cityB + " khởi hành sớm nhất.";
                    } else if (isBtoA) {
                        score = 0.88; // Round-trip return leg
                        badgeText = "Chuyến khứ hồi dự kiến 🔁";
                        reasonText = "Chuyến quay lại " + cityA + " để bạn chủ động lên kế hoạch khứ hồi thuận tiện.";
                    } else if (matchesCity(routeFrom, currentCity)) {
                        score = 0.75;
                        badgeText = "Khởi hành gần bạn 📍";
                        reasonText = "Chuyến xe xuất phát từ " + routeFrom + " đến " + routeTo + " (gần bạn).";
                    } else {
                        score = 0.20;
                        badgeText = "Tuyến xe khác";
                        reasonText = "Chuyến xe " + routeFrom + " → " + routeTo;
                    }
                } else if (matchesCity(currentCity, cityB)) {
                    // Scenario 3: User is at Base B (e.g. Phú Yên) -> Infer returning to Base A (TP.HCM)
                    if (isBtoA) {
                        score = 0.98; // TOP #1
                        badgeText = "Quay lại làm việc / Học tập 💼";
                        reasonText = "Bạn đang ở " + currentCity + " - Ưu tiên các chuyến xe trở lại " + cityA + " thuận tiện nhất.";
                    } else if (isAtoB) {
                        score = 0.88;
                        badgeText = "Chuyến khứ hồi dự kiến 🔁";
                        reasonText = "Chuyến về " + cityB + " để bạn chủ động lên kế hoạch khứ hồi.";
                    } else if (matchesCity(routeFrom, currentCity)) {
                        score = 0.75;
                        badgeText = "Khởi hành gần bạn 📍";
                        reasonText = "Chuyến xe xuất phát từ " + routeFrom + " đến " + routeTo + " (gần bạn).";
                    } else {
                        score = 0.20;
                        badgeText = "Tuyến xe khác";
                        reasonText = "Chuyến xe " + routeFrom + " → " + routeTo;
                    }
                } else {
                    // Scenario 4: User is at a 3rd City C (e.g. Đà Nẵng, Nha Trang, etc.)
                    // Check if trip departs from Current City and heads to Base A (Metropolitan Hub) or Base B (Hometown)
                    boolean toBaseA = matchesCity(routeFrom, currentCity) && matchesCity(routeTo, cityA);
                    boolean toBaseB = matchesCity(routeFrom, currentCity) && matchesCity(routeTo, cityB);

                    if (toBaseB) {
                        score = 0.96;
                        badgeText = "🏡 Về " + cityB + " (Quê nhà)";
                        reasonText = "Bạn đang ở " + currentCity + " - AI gợi ý chuyến xe trở về " + cityB + " (quê nhà / điểm đến thường lệ) thuận tiện nhất.";
                    } else if (toBaseA) {
                        score = 0.96;
                        badgeText = "💼 Về " + cityA + " (Trung tâm)";
                        reasonText = "Bạn đang ở " + currentCity + " - AI gợi ý chuyến xe đến " + cityA + " (trung tâm học tập & làm việc của bạn) xuất bến sớm nhất.";
                    } else if (matchesCity(routeFrom, currentCity)) {
                        score = 0.82;
                        badgeText = "Khởi hành gần bạn 📍";
                        reasonText = "Chuyến xe xuất phát từ vị trí hiện tại " + currentCity + " đến " + routeTo + ".";
                    } else if (isOnCorridor) {
                        score = 0.70;
                        badgeText = "Tuyến đường quen thuộc 🔄";
                        reasonText = "Tuyến đường cố định giữa " + cityA + " và " + cityB + " của bạn.";
                    } else {
                        score = 0.20;
                        badgeText = "Tuyến xe khác";
                        reasonText = "Chuyến xe " + routeFrom + " → " + routeTo;
                    }
                }

                scoredCandidates.add(buildRecommendedTripDto(trip, score, badgeText, reasonText, persona.name()));
            }

        } else {
            // =========================================================================
            // STANDARD PERSONA & GEOLOCATION-BASED RECOMMENDATION
            // =========================================================================
            for (Trip trip : allTrips) {
                double personaScore = evaluateTripAgainstPersona(trip, persona);
                double vectorScore = vectorSimilarityMap.getOrDefault(trip.getId(), 0.70d);

                double hybridScore = Math.round((0.60 * personaScore + 0.40 * vectorScore) * 100.0) / 100.0;

                String routeFrom = trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "TP.HCM";
                String routeTo = trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "Đà Lạt";

                boolean isNearbyDeparture = matchesCity(routeFrom, currentCity);
                if (currentCity != null && !currentCity.isBlank()) {
                    if (isNearbyDeparture) {
                        hybridScore = Math.min(0.99, hybridScore + 0.35);
                    } else {
                        hybridScore = Math.max(0.15, hybridScore - 0.25);
                    }
                }

                String badgeText = (isNearbyDeparture && currentCity != null && !currentCity.isBlank())
                        ? "Khởi hành gần bạn 📍"
                        : getBadgeForPersona(persona, hybridScore);

                String reasonText = (isNearbyDeparture && currentCity != null && !currentCity.isBlank())
                        ? "Chuyến xe xuất phát từ " + routeFrom + " đến " + routeTo + " (gần vị trí hiện tại của bạn)"
                        : getReasonForPersona(persona, routeFrom, routeTo);

                scoredCandidates.add(buildRecommendedTripDto(trip, hybridScore, badgeText, reasonText, persona.name()));
            }
        }

        List<RecommendedTripDto> recommendations;

        if (multiDestOpt.isPresent()) {
            recommendations = resolveMultiDestinationRecommendations(multiDestOpt.get(), allTrips, currentCity, maxLimit);
        } else if (habitOpt.isPresent()) {
            CommuterHabit habit = habitOpt.get();
            String cityA = habit.cityA();
            String cityB = habit.cityB();
            boolean hasLocation = currentCity != null && !currentCity.isBlank();

            if (!hasLocation) {
                // Scenario 1: Location OFF -> 50% City A -> City B and 50% City B -> City A
                List<RecommendedTripDto> listAtoB = scoredCandidates.stream()
                        .filter(c -> matchesCity(c.getDepartureLocation(), cityA) && matchesCity(c.getArrivalLocation(), cityB))
                        .sorted(Comparator.comparing(RecommendedTripDto::getDepartureTime))
                        .toList();
                List<RecommendedTripDto> listBtoA = scoredCandidates.stream()
                        .filter(c -> matchesCity(c.getDepartureLocation(), cityB) && matchesCity(c.getArrivalLocation(), cityA))
                        .sorted(Comparator.comparing(RecommendedTripDto::getDepartureTime))
                        .toList();

                recommendations = interleaveBalanced(listAtoB, listBtoA, scoredCandidates, maxLimit);
            } else if (!matchesCity(currentCity, cityA) && !matchesCity(currentCity, cityB)) {
                // Scenario 4: User is at 3rd City C (e.g. Nha Trang)
                // 50% City C -> City B (Phú Yên) and 50% City C -> City A (TP.HCM)!
                List<RecommendedTripDto> toCityB = scoredCandidates.stream()
                        .filter(c -> matchesCity(c.getDepartureLocation(), currentCity) && matchesCity(c.getArrivalLocation(), cityB))
                        .sorted(Comparator.comparing(RecommendedTripDto::getDepartureTime))
                        .toList();
                List<RecommendedTripDto> toCityA = scoredCandidates.stream()
                        .filter(c -> matchesCity(c.getDepartureLocation(), currentCity) && matchesCity(c.getArrivalLocation(), cityA))
                        .sorted(Comparator.comparing(RecommendedTripDto::getDepartureTime))
                        .toList();

                recommendations = interleaveBalanced(toCityB, toCityA, scoredCandidates, maxLimit);
            } else {
                scoredCandidates.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
                recommendations = scoredCandidates.stream().limit(maxLimit).collect(Collectors.toList());
            }
        } else {
            scoredCandidates.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            recommendations = scoredCandidates.stream().limit(maxLimit).collect(Collectors.toList());
        }

        // Populate exact available seats only for the selected top recommendations
        for (RecommendedTripDto dto : recommendations) {
            try {
                tripRepository.findById(dto.getTripId()).ifPresent(t -> {
                    dto.setAvailableSeats(tripBookingAvailabilityService.getAvailableSeatCount(t));
                });
            } catch (Exception ignored) {}
        }

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

    private RecommendedTripDto buildRecommendedTripDto(Trip trip, double score, String badgeText, String reasonText, String cluster) {
        Long routeId = trip.getRoute() != null ? trip.getRoute().getId() : 1L;
        String routeFrom = trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "TP.HCM";
        String routeTo = trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "Đà Lạt";
        String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getType() : "Limousine 34 phòng";
        String plate = trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : "51B-892.12";

        int availableSeats = trip.getAvailableSeats() != null ? trip.getAvailableSeats() : 28;
        try {
            availableSeats = tripBookingAvailabilityService.getAvailableSeatCount(trip);
        } catch (Exception ignored) {}

        return RecommendedTripDto.builder()
                .tripId(trip.getId())
                .routeId(routeId)
                .departureLocation(routeFrom)
                .arrivalLocation(routeTo)
                .departureTime(trip.getDepartureTime())
                .price(trip.getPrice())
                .vehicleType(vehicleType)
                .licensePlate(plate)
                .availableSeats(availableSeats)
                .matchScore(Math.round(score * 100.0) / 100.0)
                .reason(reasonText)
                .personaCluster(cluster)
                .badgeText(badgeText)
                .build();
    }

    /**
     * Identifies the primary travel habit corridor (City A <-> City B)
     * by scanning customer ticket bookings and session searches.
     * City A is designated as the Metropolitan / Work / School Hub (e.g. TP.HCM).
     * City B is designated as the Hometown / Province (e.g. Phú Yên).
     */
    public Optional<CommuterHabit> detectCommuterHabit(UUID customerId, String anonymousId) {
        Map<String, Integer> corridorCounts = new HashMap<>();
        Map<String, String[]> pairMapping = new HashMap<>();
        Map<String, Integer> directionAtoBCounts = new HashMap<>();

        // 1. Inspect Bookings (Highest confidence)
        if (customerId != null) {
            List<Booking> bookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
            for (Booking b : bookings) {
                if (b.getTrip() != null && b.getTrip().getRoute() != null) {
                    String from = b.getTrip().getRoute().getDepartureLocation();
                    String to = b.getTrip().getRoute().getArrivalLocation();
                    registerCorridor(from, to, corridorCounts, pairMapping, directionAtoBCounts, 3);
                }
            }
        }

        // 2. Inspect Actions (Search & Seat Selection)
        List<UserSession> sessions = Collections.emptyList();
        if (customerId != null) {
            sessions = sessionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (anonymousId != null && !anonymousId.isBlank()) {
            sessions = sessionRepository.findByAnonymousIdOrderByCreatedAtDesc(anonymousId);
        }

        for (UserSession s : sessions.stream().limit(10).toList()) {
            List<UserAction> actions = actionRepository.findBySessionIdOrderByCreatedAtAsc(s.getId());
            for (UserAction act : actions) {
                String payload = act.getPayload() != null ? act.getPayload().toLowerCase(Locale.ROOT) : "";
                if (payload.contains("phú yên") || payload.contains("tuy hòa")) {
                    registerCorridor("TP.HCM", "Phú Yên", corridorCounts, pairMapping, directionAtoBCounts, 2);
                }
            }
        }

        // 3. Find most frequent corridor
        if (corridorCounts.isEmpty()) {
            return Optional.empty();
        }

        Map.Entry<String, Integer> topEntry = corridorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (topEntry != null && topEntry.getValue() >= 2) {
            String[] rawCities = pairMapping.get(topEntry.getKey());
            String c1 = rawCities[0];
            String c2 = rawCities[1];

            // Resort / holiday travel corridors like Đà Lạt are governed by specialized leisure personas (e.g. DALAT_LIMOUSINE)
            if (c1.equalsIgnoreCase("Đà Lạt") || c2.equalsIgnoreCase("Đà Lạt")) {
                return Optional.empty();
            }

            String cityA, cityB;

            // Designate City A as Metropolitan Work Hub and City B as Hometown / Province
            if (METRO_HUBS.contains(c1) && !METRO_HUBS.contains(c2)) {
                cityA = c1;
                cityB = c2;
            } else if (METRO_HUBS.contains(c2) && !METRO_HUBS.contains(c1)) {
                cityA = c2;
                cityB = c1;
            } else {
                cityA = c1;
                cityB = c2;
            }

            return Optional.of(new CommuterHabit(cityA, cityB, topEntry.getValue(), cityA));
        }

        return Optional.empty();
    }

    private void registerCorridor(String from, String to,
                                  Map<String, Integer> counts,
                                  Map<String, String[]> pairMapping,
                                  Map<String, Integer> dirCounts,
                                  int weight) {
        if (from == null || to == null) return;
        String nFrom = canonicalCityName(from);
        String nTo = canonicalCityName(to);
        if (nFrom.equalsIgnoreCase(nTo)) return;

        String key = nFrom.compareTo(nTo) < 0 ? (nFrom + "<->" + nTo) : (nTo + "<->" + nFrom);
        counts.put(key, counts.getOrDefault(key, 0) + weight);
        if (!pairMapping.containsKey(key)) {
            pairMapping.put(key, new String[]{nFrom, nTo});
        }
        dirCounts.put(nFrom + "->" + nTo, dirCounts.getOrDefault(nFrom + "->" + nTo, 0) + weight);
    }

    private String canonicalCityName(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("sài gòn") || lower.contains("tp.hcm") || lower.contains("hồ chí minh") || lower.contains("tphcm")) {
            return "TP.HCM";
        }
        if (lower.contains("phú yên") || lower.contains("tuy hòa")) {
            return "Phú Yên";
        }
        if (lower.contains("đà lạt") || lower.contains("da lat")) {
            return "Đà Lạt";
        }
        if (lower.contains("nha trang")) {
            return "Nha Trang";
        }
        if (lower.contains("hà nội") || lower.contains("ha noi")) {
            return "Hà Nội";
        }
        if (lower.contains("đà nẵng") || lower.contains("da nang")) {
            return "Đà Nẵng";
        }
        if (lower.contains("cần thơ") || lower.contains("can tho")) {
            return "Cần Thơ";
        }
        if (lower.contains("vũng tàu") || lower.contains("vung tau")) {
            return "Vũng Tàu";
        }
        return raw.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaType inferUserPersona(String anonymousId, UUID customerId, String ipAddress) {
        if (customerId != null) {
            Optional<MultiDestinationHabit> multi = detectMultiDestinationHabit(customerId);
            if (multi.isPresent()) {
                return PersonaType.EXPLORER_MULTI_DESTINATION;
            }
        }

        Optional<CommuterHabit> habit = detectCommuterHabit(customerId, anonymousId);
        if (habit.isPresent()) {
            return PersonaType.COMMUTER_HABIT;
        }

        List<UserSession> sessions = Collections.emptyList();
        if (customerId != null) {
            sessions = sessionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (anonymousId != null && !anonymousId.isBlank()) {
            sessions = sessionRepository.findByAnonymousIdOrderByCreatedAtDesc(anonymousId);
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
            if (session.getUtmCampaign() != null) {
                String c = session.getUtmCampaign().toLowerCase();
                if (c.contains("dalat") || c.contains("limousine")) dalatLimousinePoints += 3;
                if (c.contains("nhatrang") || c.contains("family")) nhaTrangPoints += 3;
                if (c.contains("tietkiem") || c.contains("sinhvien") || c.contains("sale")) budgetSaverPoints += 3;
            }

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
        String vehicle = trip.getVehicle() != null ? trip.getVehicle().getType().toLowerCase() : "";
        int hour = trip.getDepartureTime() != null ? trip.getDepartureTime().getHour() : 12;
        BigDecimal price = trip.getPrice() != null ? trip.getPrice() : BigDecimal.valueOf(250000);

        switch (persona) {
            case DALAT_LIMOUSINE:
                double dScore = 0.50;
                if (dest.contains("lạt") || dest.contains("dalat")) dScore += 0.30;
                if (vehicle.contains("limousine") || vehicle.contains("phòng") || vehicle.contains("vip")) dScore += 0.15;
                if (hour >= 20 || hour <= 1) dScore += 0.05;
                return Math.min(1.0, dScore);

            case NHATRANG_FAMILY:
                double nScore = 0.50;
                if (dest.contains("nha trang") || dest.contains("phan thiết") || dest.contains("quy nhơn")) nScore += 0.30;
                if (hour >= 6 && hour <= 17) nScore += 0.10;
                if (vehicle.contains("giường nằm") || vehicle.contains("40")) nScore += 0.10;
                return Math.min(1.0, nScore);

            case BUDGET_SAVER:
                double bScore = 0.50;
                if (price.compareTo(BigDecimal.valueOf(220000)) <= 0) bScore += 0.35;
                if (dest.contains("vũng tàu") || dest.contains("cần thơ") || dest.contains("hải phòng")) bScore += 0.15;
                return Math.min(1.0, bScore);

            case COMMUTER_HABIT:
                return 0.90;

            case POPULAR_TRENDING:
            default:
                if (dest.contains("lạt") || dest.contains("sapa") || dest.contains("nha trang")) return 0.90;
                return 0.75;
        }
    }

    private String buildSemanticQuery(PersonaType persona, CommuterHabit habit) {
        if (habit != null) {
            return "Xe khách chạy tuyến cố định khứ hồi " + habit.cityA() + " " + habit.cityB() + " giường nằm Limousine đúng giờ êm ái";
        }
        return switch (persona) {
            case EXPLORER_MULTI_DESTINATION -> "Xe khách giường nằm Limousine chất lượng cao các tuyến du lịch hot Đà Lạt Nha Trang Vũng Tàu Phú Yên";
            case COMMUTER_HABIT -> "Xe khách chạy tuyến cố định khứ hồi Sài Gòn Phú Yên Tuy Hòa giường nằm Limousine";
            case DALAT_LIMOUSINE -> "Xe Limousine phòng nằm cao cấp tuyến Sài Gòn Đà Lạt chạy ban đêm 21h tiện nghi wifi sạc";
            case NHATRANG_FAMILY -> "Xe giường nằm chất lượng cao đi Nha Trang khởi hành ban ngày thoải mái êm ái gia đình";
            case BUDGET_SAVER -> "Vé xe khách giá rẻ tiết kiệm dưới 200k tuyến Vũng Tàu Cần Thơ ghế ngồi giường nằm";
            case POPULAR_TRENDING -> "Tuyến xe du lịch hot nhất Sài Gòn Đà Lạt Hà Nội Sapa xe đời mới";
        };
    }

    private String getBadgeForPersona(PersonaType persona, double score) {
        int percent = (int) Math.round(score * 100);
        return switch (persona) {
            case EXPLORER_MULTI_DESTINATION -> "🌟 Top điểm đến quen thuộc của bạn";
            case COMMUTER_HABIT -> "🔄 Tuyến quen thuộc thường đi";
            case DALAT_LIMOUSINE -> "🌟 Tuyến ưa chuộng: " + Math.max(82, percent) + "% khách chọn";
            case NHATRANG_FAMILY -> "✨ Phù hợp gia đình & công tác";
            case BUDGET_SAVER -> "💰 Giá tốt nhất theo thói quen";
            case POPULAR_TRENDING -> "🔥 Tuyến xe thịnh hành tuần này";
        };
    }

    private String getReasonForPersona(PersonaType persona, String from, String to) {
        return switch (persona) {
            case EXPLORER_MULTI_DESTINATION -> "Gợi ý AI: Dựa trên lịch sử khám phá nhiều nơi của bạn, tuyến " + from + " → " + to + " là một trong những điểm đến hàng đầu.";
            case COMMUTER_HABIT -> "Gợi ý AI: Dựa trên thói quen thường xuyên di chuyển tuyến " + from + " → " + to + " của bạn.";
            case DALAT_LIMOUSINE -> "Cùng sở thích: 88% khách hàng chuộng xe Limousine Đà Lạt đêm đã đặt vé chuyến " + from + " → " + to + " này.";
            case NHATRANG_FAMILY -> "Phù hợp gia đình: 92% hành khách đi cùng người thân hài lòng với lịch trình êm ái của tuyến " + from + " → " + to + ".";
            case BUDGET_SAVER -> "Gợi ý AI: Mức giá tối ưu và tiết kiệm nhất cho tuyến " + from + " → " + to + " dựa trên hành vi tìm kiếm.";
            case POPULAR_TRENDING -> "Xu hướng: Chuyến " + from + " → " + to + " đang có lượng đặt vé cao nhất trong 24 giờ qua.";
        };
    }

    @Override
    @Transactional
    public void invalidateAndRefreshUser(String anonymousId, UUID customerId, String ipAddress) {
        String cacheKey = getCacheKey(anonymousId, customerId, ipAddress, null);
        if (redisTemplate != null && redisTemplate.isPresent()) {
            try {
                redisTemplate.get().delete(cacheKey);
            } catch (Exception e) {
                log.warn("[Recommendation] Redis cache invalidation error: {}", e.getMessage());
            }
        }
        try {
            getPersonalizedRecommendations(anonymousId, customerId, ipAddress, 5);
            log.info("[Recommendation] Real-time event refreshed recommendation cache for {}", cacheKey);
        } catch (Exception e) {
            log.warn("[Recommendation] Real-time refresh warning: {}", e.getMessage());
        }
    }

    private String getCacheKey(String anonymousId, UUID customerId, String ipAddress, String currentCity) {
        String base = (customerId != null ? "USER:" + customerId : (anonymousId != null && !anonymousId.isBlank() ? "ANON:" + anonymousId : (ipAddress != null && !ipAddress.isBlank() ? "IP:" + ipAddress : "DEFAULT")));
        return REDIS_PREFIX + base + (currentCity != null && !currentCity.isBlank() ? ":CITY_" + currentCity.trim().toLowerCase(Locale.ROOT) : "");
    }

    private boolean matchesCity(String location, String targetCity) {
        if (location == null || targetCity == null || targetCity.isBlank()) return false;
        String l = location.toLowerCase(Locale.ROOT).trim();
        String t = targetCity.toLowerCase(Locale.ROOT).trim();

        // TP.HCM, Sai Gon, Sài Gòn, TP. Hồ Chí Minh
        boolean isHcmTarget = t.contains("hcm") || t.contains("sài gòn") || t.contains("sai gon") || t.contains("hồ chí minh");
        boolean isHcmLoc = l.contains("hcm") || l.contains("sài gòn") || l.contains("sai gon") || l.contains("hồ chí minh");
        if (isHcmTarget && isHcmLoc) return true;

        // Phu Yen & Tuy Hoa
        boolean isPyTarget = t.contains("phú yên") || t.contains("phu yen") || t.contains("tuy hòa") || t.contains("tuy hoa");
        boolean isPyLoc = l.contains("phú yên") || l.contains("phu yen") || l.contains("tuy hòa") || l.contains("tuy hoa");
        if (isPyTarget && isPyLoc) return true;

        // Hanoi
        boolean isHnTarget = t.contains("hà nội") || t.contains("ha noi");
        boolean isHnLoc = l.contains("hà nội") || l.contains("ha noi");
        if (isHnTarget && isHnLoc) return true;

        // Da Nang
        boolean isDnTarget = t.contains("đà nẵng") || t.contains("da nang");
        boolean isDnLoc = l.contains("đà nẵng") || l.contains("da nang");
        if (isDnTarget && isDnLoc) return true;

        // Can Tho
        boolean isCtTarget = t.contains("cần thơ") || t.contains("can tho");
        boolean isCtLoc = l.contains("cần thơ") || l.contains("can tho");
        if (isCtTarget && isCtLoc) return true;

        // Nha Trang
        if (t.contains("nha trang") && l.contains("nha trang")) return true;

        // Da Lat
        if ((t.contains("đà lạt") || t.contains("da lat")) && (l.contains("đà lạt") || l.contains("da lat"))) return true;

        // Hai Phong
        if ((t.contains("hải phòng") || t.contains("hai phong")) && (l.contains("hải phòng") || l.contains("hai phong"))) return true;

        // Vung Tau
        if ((t.contains("vũng tàu") || t.contains("vung tau")) && (l.contains("vũng tàu") || l.contains("vung tau"))) return true;

        // Quy Nhon
        if ((t.contains("quy nhơn") || t.contains("quy nhon")) && (l.contains("quy nhơn") || l.contains("quy nhon"))) return true;

        return l.contains(t) || t.contains(l);
    }

    private List<RecommendedTripDto> interleaveBalanced(
            List<RecommendedTripDto> list1,
            List<RecommendedTripDto> list2,
            List<RecommendedTripDto> allCandidates,
            int maxLimit) {

        List<RecommendedTripDto> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        int i1 = 0, i2 = 0;
        while (result.size() < maxLimit && (i1 < list1.size() || i2 < list2.size())) {
            if (i1 < list1.size()) {
                RecommendedTripDto trip1 = list1.get(i1++);
                if (seenIds.add(trip1.getTripId())) {
                    result.add(trip1);
                }
            }
            if (result.size() < maxLimit && i2 < list2.size()) {
                RecommendedTripDto trip2 = list2.get(i2++);
                if (seenIds.add(trip2.getTripId())) {
                    result.add(trip2);
                }
            }
        }

        if (result.size() < maxLimit) {
            List<RecommendedTripDto> leftovers = allCandidates.stream()
                    .filter(c -> !seenIds.contains(c.getTripId()))
                    .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                    .toList();
            for (RecommendedTripDto dto : leftovers) {
                if (result.size() >= maxLimit) break;
                if (seenIds.add(dto.getTripId())) {
                    result.add(dto);
                }
            }
        }

        return result;
    }


    public Optional<MultiDestinationHabit> detectMultiDestinationHabit(UUID customerId) {
        if (customerId == null) {
            return Optional.empty();
        }

        List<Booking> bookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        if (bookings == null || bookings.size() < 5) {
            return Optional.empty();
        }

        Map<String, Integer> departureCounts = new HashMap<>();
        Map<String, Integer> destinationVisits = new HashMap<>();
        Set<String> allCities = new HashSet<>();

        for (Booking b : bookings) {
            if (b.getTrip() != null && b.getTrip().getRoute() != null) {
                String from = canonicalCityName(b.getTrip().getRoute().getDepartureLocation());
                String to = canonicalCityName(b.getTrip().getRoute().getArrivalLocation());
                if (from == null || to == null) continue;

                departureCounts.put(from, departureCounts.getOrDefault(from, 0) + 1);
                allCities.add(from);
                allCities.add(to);
            }
        }

        String primaryHomeBase = departureCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("TP.HCM");

        for (Booking b : bookings) {
            if (b.getTrip() != null && b.getTrip().getRoute() != null) {
                String from = canonicalCityName(b.getTrip().getRoute().getDepartureLocation());
                String to = canonicalCityName(b.getTrip().getRoute().getArrivalLocation());
                if (from == null || to == null) continue;

                if (!to.equalsIgnoreCase(primaryHomeBase)) {
                    destinationVisits.put(to, destinationVisits.getOrDefault(to, 0) + 1);
                } else if (!from.equalsIgnoreCase(primaryHomeBase)) {
                    destinationVisits.put(from, destinationVisits.getOrDefault(from, 0) + 1);
                }
            }
        }

        if (destinationVisits.size() < 3 || allCities.size() < 4) {
            return Optional.empty();
        }

        List<Map.Entry<String, Integer>> sorted = destinationVisits.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        List<TopDestination> topDests = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            topDests.add(new TopDestination(entry.getKey(), entry.getValue(), rank++));
        }

        return Optional.of(new MultiDestinationHabit(
                primaryHomeBase,
                topDests,
                bookings.size(),
                destinationVisits.size()
        ));
    }

    private List<RecommendedTripDto> resolveMultiDestinationRecommendations(
            MultiDestinationHabit multiHabit,
            List<Trip> allTrips,
            String currentCity,
            int maxLimit) {

        String homeBase = multiHabit.primaryHomeBase();
        String effectiveDep = (currentCity != null && !currentCity.isBlank()) ? currentCity.trim() : homeBase;

        List<RecommendedTripDto> result = new ArrayList<>();
        Set<Long> pickedTripIds = new HashSet<>();

        for (TopDestination td : multiHabit.topDestinations()) {
            if (result.size() >= maxLimit) break;

            String targetDest = td.cityName();
            boolean isUserAtDest = matchesCity(effectiveDep, targetDest);

            Trip selectedTrip = null;
            String badgeText;
            String reasonText;
            double score = Math.max(0.85, 0.98 - (td.rank() - 1) * 0.02);

            String icon = td.rank() == 1 ? "🔥" : (td.rank() == 2 ? "🌟" : (td.rank() == 3 ? "🏖️" : "🏡"));

            if (isUserAtDest) {
                // User is physically AT this favorite destination -> Offer return trip back to primary homeBase
                selectedTrip = allTrips.stream()
                        .filter(t -> !pickedTripIds.contains(t.getId()))
                        .filter(t -> t.getRoute() != null
                                && matchesCity(t.getRoute().getDepartureLocation(), effectiveDep)
                                && matchesCity(t.getRoute().getArrivalLocation(), homeBase))
                        .min(Comparator.comparing(Trip::getDepartureTime))
                        .orElse(null);

                badgeText = icon + " Điểm đến #" + td.rank() + " (" + targetDest + ") ➜ Về " + homeBase;
                reasonText = "Bạn đang ở " + targetDest + " (Điểm đến yêu thích #" + td.rank() + " với " + td.visitCount() + " chuyến của bạn) - AI gợi ý chuyến xe trở về " + homeBase + " (điểm xuất phát quen thuộc của bạn) xuất bến sớm nhất.";
            } else {
                selectedTrip = allTrips.stream()
                        .filter(t -> !pickedTripIds.contains(t.getId()))
                        .filter(t -> t.getRoute() != null
                                && matchesCity(t.getRoute().getDepartureLocation(), effectiveDep)
                                && matchesCity(t.getRoute().getArrivalLocation(), targetDest))
                        .min(Comparator.comparing(Trip::getDepartureTime))
                        .orElse(null);

                boolean departsFromEffective = selectedTrip != null;

                if (selectedTrip == null && !matchesCity(effectiveDep, homeBase)) {
                    selectedTrip = allTrips.stream()
                            .filter(t -> !pickedTripIds.contains(t.getId()))
                            .filter(t -> t.getRoute() != null
                                    && matchesCity(t.getRoute().getDepartureLocation(), homeBase)
                                    && matchesCity(t.getRoute().getArrivalLocation(), targetDest))
                            .min(Comparator.comparing(Trip::getDepartureTime))
                            .orElse(null);
                }

                badgeText = icon + " Điểm đến #" + td.rank() + " (" + targetDest + " - " + td.visitCount() + " chuyến)";
                if (departsFromEffective && !matchesCity(effectiveDep, homeBase)) {
                    reasonText = "Bạn đang ở " + effectiveDep + " - AI gợi ý chuyến xe kết nối trực tiếp đến " + targetDest
                            + " (Điểm đến yêu thích #" + td.rank() + " của bạn với " + td.visitCount() + " chuyến).";
                } else {
                    reasonText = "Dựa trên " + multiHabit.totalBookings() + " vé đã đặt của bạn: " + targetDest
                            + " là điểm đến bạn ghé thăm nhiều thứ " + td.rank() + " (" + td.visitCount() + " chuyến). AI ưu tiên giờ chạy đẹp nhất cho bạn.";
                }
            }

            if (selectedTrip != null) {
                pickedTripIds.add(selectedTrip.getId());
                result.add(buildRecommendedTripDto(selectedTrip, score, badgeText, reasonText, PersonaType.EXPLORER_MULTI_DESTINATION.name()));
            }
        }

        if (result.size() < maxLimit) {
            for (Trip trip : allTrips) {
                if (result.size() >= maxLimit) break;
                if (!pickedTripIds.contains(trip.getId())) {
                    pickedTripIds.add(trip.getId());
                    result.add(buildRecommendedTripDto(trip, 0.80, "✨ Tuyến xe phổ biến",
                            "Gợi ý bổ sung phù hợp với thói quen di chuyển phong phú của bạn.", PersonaType.EXPLORER_MULTI_DESTINATION.name()));
                }
            }
        }

        return result;
    }

}
