package vn.edu.iuh.nhom01_www.tracking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.tracking.dto.*;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserRecommendation;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserRecommendationRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.recommendation.service.RecommendationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements vn.edu.iuh.nhom01_www.tracking.service.TrackingService {

    private final UserSessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final UserActionRepository actionRepository;
    private final UserRecommendationRepository recommendationRepository;
    private final CustomerRepository customerRepository;
    private final Optional<RecommendationService> recommendationService;
    private final vn.edu.iuh.nhom01_www.trip.repository.TripRepository tripRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public UserSession getOrCreateSession(TrackSessionRequest request, String ipAddress, String userAgent) {
        String anonId = (request != null && request.getAnonymousId() != null && !request.getAnonymousId().isBlank())
                ? request.getAnonymousId()
                : "anon-" + UUID.randomUUID();

        UUID customerId = request != null ? request.getCustomerId() : null;
        String trafficSource = (request != null && request.getTrafficSource() != null && !request.getTrafficSource().isBlank())
                ? request.getTrafficSource()
                : "direct";
        String campaign = request != null ? request.getUtmCampaign() : null;

        // Try to find an active recent session (within 30 minutes)
        Optional<UserSession> existing = Optional.empty();
        if (customerId != null) {
            existing = sessionRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId);
        } else {
            existing = sessionRepository.findTopByAnonymousIdOrderByCreatedAtDesc(anonId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing.isPresent()) {
            UserSession session = existing.get();
            LocalDateTime lastActive = session.getEndTime() != null ? session.getEndTime() : session.getStartTime();
            if (lastActive != null && lastActive.isAfter(now.minusMinutes(30))) {
                session.setEndTime(now);
                if (customerId != null && session.getCustomerId() == null) {
                    session.setCustomerId(customerId);
                }
                return sessionRepository.save(session);
            }
        }

        UserSession newSession = UserSession.builder()
                .anonymousId(anonId)
                .customerId(customerId)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .userAgent(userAgent != null ? (userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent) : "Unknown")
                .trafficSource(trafficSource)
                .utmCampaign(campaign)
                .startTime(now)
                .endTime(now)
                .totalDurationSeconds(0)
                .build();

        return sessionRepository.save(newSession);
    }

    @Override
    @Transactional
    public PageView recordPageView(TrackPageViewRequest request) {
        if (request == null || request.getSessionId() == null) {
            throw new IllegalArgumentException("Session ID must not be null");
        }

        UserSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        sessionRepository.save(session);

        PageView pageView = PageView.builder()
                .session(session)
                .pageUrl(request.getPageUrl() != null ? request.getPageUrl() : "/")
                .pageTitle(request.getPageTitle() != null ? request.getPageTitle() : "VEXE")
                .previousPageUrl(request.getPreviousPageUrl())
                .enteredAt(now)
                .activeDurationSeconds(0)
                .idleDurationSeconds(0)
                .build();

        return pageViewRepository.save(pageView);
    }

    @Override
    @Transactional
    public void updateHeartbeat(TrackHeartbeatRequest request) {
        if (request == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (request.getSessionId() != null) {
            sessionRepository.findById(request.getSessionId()).ifPresent(session -> {
                session.setEndTime(now);
                if (request.getAdditionalActiveSeconds() != null) {
                    int current = session.getTotalDurationSeconds() != null ? session.getTotalDurationSeconds() : 0;
                    session.setTotalDurationSeconds(current + request.getAdditionalActiveSeconds());
                }
                sessionRepository.save(session);
            });
        }

        if (request.getPageViewId() != null) {
            pageViewRepository.findById(request.getPageViewId()).ifPresent(pv -> {
                pv.setLeftAt(now);
                if (request.getAdditionalActiveSeconds() != null) {
                    int active = pv.getActiveDurationSeconds() != null ? pv.getActiveDurationSeconds() : 0;
                    pv.setActiveDurationSeconds(active + request.getAdditionalActiveSeconds());
                }
                if (request.getAdditionalIdleSeconds() != null) {
                    int idle = pv.getIdleDurationSeconds() != null ? pv.getIdleDurationSeconds() : 0;
                    pv.setIdleDurationSeconds(idle + request.getAdditionalIdleSeconds());
                }
                pageViewRepository.save(pv);
            });
        }
    }

    @Override
    @Transactional
    public UserAction recordAction(TrackActionRequest request) {
        if (request == null || request.getSessionId() == null) {
            throw new IllegalArgumentException("Session ID is required for recording action");
        }

        UserSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        PageView pageView = null;
        if (request.getPageViewId() != null) {
            pageView = pageViewRepository.findById(request.getPageViewId()).orElse(null);
        }

        UserAction action = UserAction.builder()
                .session(session)
                .pageView(pageView)
                .actionType(request.getActionType() != null ? request.getActionType().toUpperCase() : "GENERAL_CLICK")
                .payload(request.getPayload())
                .build();

        UserAction savedAction = actionRepository.save(action);

        // Real-time Event-driven Recommendation Refresh
        String type = savedAction.getActionType();
        if (type.contains("SEARCH") || type.contains("TRIP") || type.contains("SEAT") || type.contains("CHECKOUT") || type.contains("RECOMMENDATION")) {
            if (recommendationService != null && recommendationService.isPresent()) {
                try {
                    recommendationService.get().invalidateAndRefreshUser(
                            session.getAnonymousId(),
                            session.getCustomerId(),
                            session.getIpAddress()
                    );
                } catch (Exception e) {
                    log.debug("[Tracking] Real-time rec refresh error: {}", e.getMessage());
                }
            }
        }

        return savedAction;
    }

    @Override
    @Transactional
    public void mergeCustomerSession(UUID sessionId, UUID customerId) {
        if (sessionId == null || customerId == null) return;
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCustomerId(customerId);
            sessionRepository.save(session);

            // Identity Resolution: Merge all past sessions from this anonymousId to the customer
            if (session.getAnonymousId() != null && !session.getAnonymousId().isBlank()) {
                List<UserSession> anonSessions = sessionRepository.findByAnonymousIdOrderByCreatedAtDesc(session.getAnonymousId());
                for (UserSession s : anonSessions) {
                    if (s.getCustomerId() == null) {
                        s.setCustomerId(customerId);
                        sessionRepository.save(s);
                    }
                }
            }
            log.info("[Tracking] Merged guest session {} & anonymousId {} with customer {}", 
                    sessionId, session.getAnonymousId(), customerId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingAnalyticsResponse getAnalyticsOverview() {
        long totalSessions = sessionRepository.count();
        long uniqueVisitors = sessionRepository.countUniqueVisitors();
        Double avgDuration = sessionRepository.getAverageSessionDuration();

        // Traffic sources breakdown
        Map<String, Long> trafficSources = new LinkedHashMap<>();
        for (Object[] row : sessionRepository.countByTrafficSource()) {
            String source = row[0] != null ? row[0].toString() : "direct";
            Long count = row[1] != null ? (Long) row[1] : 0L;
            trafficSources.put(source, count);
        }

        // Action types breakdown
        Map<String, Long> actionBreakdown = new LinkedHashMap<>();
        for (Object[] row : actionRepository.countByActionType()) {
            String action = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = row[1] != null ? (Long) row[1] : 0L;
            actionBreakdown.put(action, count);
        }

        // Top pages
        List<Object[]> pageRows = pageViewRepository.getPageDurationStats(PageRequest.of(0, 10));
        List<TrackingAnalyticsResponse.PageStatDto> topPages = pageRows.stream().map(row -> {
            String url = row[0] != null ? row[0].toString() : "/";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            double avgActive = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double avgIdle = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            return TrackingAnalyticsResponse.PageStatDto.builder()
                    .pageUrl(url)
                    .viewCount(count)
                    .avgActiveSeconds(Math.round(avgActive * 10.0) / 10.0)
                    .avgIdleSeconds(Math.round(avgIdle * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());

        // Recent sessions
        List<UserSession> recent = sessionRepository.findRecentSessions(PageRequest.of(0, 15));
        List<TrackingAnalyticsResponse.RecentSessionDto> recentDtos = recent.stream().map(s -> {
            int pCount = s.getPageViews() != null ? s.getPageViews().size() : 0;
            int aCount = s.getActions() != null ? s.getActions().size() : 0;
            return TrackingAnalyticsResponse.RecentSessionDto.builder()
                    .sessionId(s.getId().toString())
                    .anonymousId(s.getAnonymousId())
                    .customerId(s.getCustomerId() != null ? s.getCustomerId().toString() : null)
                    .ipAddress(s.getIpAddress())
                    .trafficSource(s.getTrafficSource())
                    .totalDurationSeconds(s.getTotalDurationSeconds())
                    .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().format(ISO_FORMATTER) : "")
                    .pageViewCount(pCount)
                    .actionCount(aCount)
                    .build();
        }).collect(Collectors.toList());

        return TrackingAnalyticsResponse.builder()
                .totalSessions(totalSessions)
                .uniqueVisitors(uniqueVisitors)
                .avgSessionDurationSeconds(avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : 0.0)
                .trafficSources(trafficSources)
                .actionBreakdown(actionBreakdown)
                .topPages(topPages)
                .recentSessions(recentDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerJourneyTimelineDto getCustomerJourney(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return CustomerJourneyTimelineDto.builder()
                    .identifier("Unknown")
                    .customerName("Không xác định")
                    .customerEmail("N/A")
                    .isRegisteredCustomer(false)
                    .linkedAnonymousIds(Collections.emptyList())
                    .events(Collections.emptyList())
                    .journeys(Collections.emptyList())
                    .build();
        }

        Customer identifiedCustomer = null;
        UUID parsedUuid = null;
        try {
            parsedUuid = UUID.fromString(identifier.trim());
        } catch (IllegalArgumentException ignored) {}

        // 1. Identity Resolution by Email
        if (identifier.contains("@")) {
            identifiedCustomer = customerRepository.findByEmail(identifier.trim());
        } else if (parsedUuid != null) {
            Optional<Customer> custOpt = customerRepository.findById(parsedUuid);
            if (custOpt.isPresent()) {
                identifiedCustomer = custOpt.get();
            }
        }

        Set<UserSession> resolvedSessions = new LinkedHashSet<>();
        Set<String> linkedAnonymousIds = new LinkedHashSet<>();

        // 2. Identity Graph Stitching
        if (identifiedCustomer != null) {
            UUID custId = identifiedCustomer.getId();
            List<UserSession> custSessions = sessionRepository.findByCustomerIdOrderByCreatedAtDesc(custId);
            resolvedSessions.addAll(custSessions);

            List<String> anonIds = sessionRepository.findDistinctAnonymousIdsByCustomerId(custId);
            linkedAnonymousIds.addAll(anonIds);
            if (!anonIds.isEmpty()) {
                List<UserSession> anonSessions = sessionRepository.findByAnonymousIdInOrderByCreatedAtDesc(anonIds);
                resolvedSessions.addAll(anonSessions);
            }
        } else if (parsedUuid != null) {
            // Might be a session ID
            sessionRepository.findById(parsedUuid).ifPresent(s -> {
                resolvedSessions.add(s);
                if (s.getCustomerId() != null) {
                    customerRepository.findById(s.getCustomerId()).ifPresent(c -> {
                        resolvedSessions.addAll(sessionRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId()));
                    });
                }
            });
        } else if (identifier.startsWith("anon-")) {
            List<UserSession> anonSessions = sessionRepository.findByAnonymousIdOrderByCreatedAtDesc(identifier.trim());
            resolvedSessions.addAll(anonSessions);
            linkedAnonymousIds.add(identifier.trim());

            // Check if any session is linked to a registered customer!
            for (UserSession s : anonSessions) {
                if (s.getCustomerId() != null) {
                    Optional<Customer> c = customerRepository.findById(s.getCustomerId());
                    if (c.isPresent()) {
                        identifiedCustomer = c.get();
                        resolvedSessions.addAll(sessionRepository.findByCustomerIdOrderByCreatedAtDesc(identifiedCustomer.getId()));
                        break;
                    }
                }
            }
        } else {
            // IP address fallback
            List<UserSession> ipSessions = sessionRepository.findByIpAddressOrderByCreatedAtDesc(identifier.trim());
            resolvedSessions.addAll(ipSessions);
        }

        // Sort all sessions chronologically (oldest to newest)
        List<UserSession> sortedSessions = resolvedSessions.stream()
                .sorted(Comparator.comparing(s -> s.getCreatedAt() != null ? s.getCreatedAt() : LocalDateTime.MIN))
                .collect(Collectors.toList());

        boolean isRegistered = (identifiedCustomer != null);
        String customerName = isRegistered ? identifiedCustomer.getFullName() : ("Khách vãng lai (" + identifier + ")");
        String customerEmail = isRegistered ? identifiedCustomer.getEmail() : "Chưa đăng ký";

        // Persona matching
        String matchedPersona = "Chưa xác định";
        List<UserRecommendation> recs = Collections.emptyList();
        if (identifiedCustomer != null) {
            recs = recommendationRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(identifiedCustomer.getId());
        } else if (identifier.startsWith("anon-")) {
            recs = recommendationRepository.findTop5ByAnonymousIdOrderByCreatedAtDesc(identifier);
        }
        if (!recs.isEmpty() && recs.get(0).getPersonaCluster() != null) {
            matchedPersona = recs.get(0).getPersonaCluster();
        }

        // 3. Extract all events across sessions
        List<CustomerJourneyTimelineDto.JourneyEventDto> allEvents = new ArrayList<>();
        int totalPageViews = 0;
        int totalActions = 0;

        for (UserSession session : sortedSessions) {
            allEvents.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                    .id("sess-" + session.getId())
                    .type("SESSION_START")
                    .title("Bắt đầu phiên truy cập")
                    .description("Nguồn: " + session.getTrafficSource() + " | IP: " + session.getIpAddress())
                    .timestamp(session.getStartTime() != null ? session.getStartTime().format(ISO_FORMATTER) : (session.getCreatedAt() != null ? session.getCreatedAt().format(ISO_FORMATTER) : ""))
                    .activeSeconds(session.getTotalDurationSeconds())
                    .idleSeconds(0)
                    .metadata("TrafficSource: " + session.getTrafficSource() + " | Campaign: " + (session.getUtmCampaign() != null ? session.getUtmCampaign() : "None"))
                    .build());

            List<PageView> pvs = pageViewRepository.findBySessionIdOrderByEnteredAtAsc(session.getId());
            totalPageViews += pvs.size();
            for (PageView pv : pvs) {
                allEvents.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                        .id("pv-" + pv.getId())
                        .type("PAGE_VIEW")
                        .title("Xem trang: " + (pv.getPageTitle() != null ? pv.getPageTitle() : pv.getPageUrl()))
                        .description("URL: " + pv.getPageUrl() + (pv.getPreviousPageUrl() != null ? " (Từ: " + pv.getPreviousPageUrl() + ")" : ""))
                        .timestamp(pv.getEnteredAt() != null ? pv.getEnteredAt().format(ISO_FORMATTER) : "")
                        .activeSeconds(pv.getActiveDurationSeconds())
                        .idleSeconds(pv.getIdleDurationSeconds())
                        .metadata("Treo tab: " + pv.getIdleDurationSeconds() + "s")
                        .build());
            }

            List<UserAction> acts = actionRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
            totalActions += acts.size();
            for (UserAction act : acts) {
                allEvents.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                        .id("act-" + act.getId())
                        .type("ACTION")
                        .title("Hành động: " + act.getActionType())
                        .description("Chi tiết: " + (act.getPayload() != null ? act.getPayload() : ""))
                        .timestamp(act.getCreatedAt() != null ? act.getCreatedAt().format(ISO_FORMATTER) : "")
                        .activeSeconds(0)
                        .idleSeconds(0)
                        .metadata("ActionType: " + act.getActionType())
                        .build());
            }
        }

        // Sort all events strictly by timestamp ASC
        allEvents.sort(Comparator.comparing(e -> e.getTimestamp() != null ? e.getTimestamp() : ""));

        // 4. Intent-Aware Dynamic Journey Demarcation
        List<CustomerJourneyTimelineDto.JourneyClusterDto> journeyClusters = segmentEventsIntoJourneys(allEvents, isRegistered);

        return CustomerJourneyTimelineDto.builder()
                .identifier(identifier)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .isRegisteredCustomer(isRegistered)
                .linkedAnonymousIds(new ArrayList<>(linkedAnonymousIds))
                .totalSessions(sortedSessions.size())
                .totalPageViews(totalPageViews)
                .totalActions(totalActions)
                .matchedPersona(matchedPersona)
                .events(allEvents)
                .journeys(journeyClusters)
                .build();
    }

    private List<CustomerJourneyTimelineDto.JourneyClusterDto> segmentEventsIntoJourneys(
            List<CustomerJourneyTimelineDto.JourneyEventDto> events, boolean isRegistered) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<CustomerJourneyTimelineDto.JourneyClusterDto> clusters = new ArrayList<>();
        List<CustomerJourneyTimelineDto.JourneyEventDto> currentClusterEvents = new ArrayList<>();

        String currentTargetRoute = null;
        String currentTrafficSource = "direct";
        boolean previousWasConverted = false;
        LocalDateTime lastEventTime = null;

        for (CustomerJourneyTimelineDto.JourneyEventDto evt : events) {
            LocalDateTime evtTime = parseTime(evt.getTimestamp());
            String detectedRoute = extractRouteFromEvent(evt);
            boolean isConversionEvent = isConversion(evt);

            boolean shouldSplit = false;
            if (!currentClusterEvents.isEmpty() && lastEventTime != null && evtTime != null) {
                long minutesDiff = java.time.Duration.between(lastEventTime, evtTime).abs().toMinutes();

                // Rule 1: Conversion boundary - Previous event finalized booking
                if (previousWasConverted) {
                    shouldSplit = true;
                }
                // Rule 2: Adaptive Inactivity Window (> 45 minutes)
                else if (minutesDiff > 45) {
                    // Route continuity: Same route intent within 120 minutes remains in same journey
                    if (currentTargetRoute != null && currentTargetRoute.equalsIgnoreCase(detectedRoute) && minutesDiff <= 120) {
                        shouldSplit = false;
                    } else {
                        shouldSplit = true;
                    }
                }
                // Rule 3: Explicit Route Shift after inactivity > 20 mins
                else if (detectedRoute != null && currentTargetRoute != null 
                        && !detectedRoute.equalsIgnoreCase(currentTargetRoute) && minutesDiff > 20) {
                    shouldSplit = true;
                }
            }

            if (shouldSplit && !currentClusterEvents.isEmpty()) {
                clusters.add(buildCluster(clusters.size() + 1, currentClusterEvents, currentTargetRoute, currentTrafficSource, isRegistered));
                currentClusterEvents = new ArrayList<>();
                currentTargetRoute = null;
                previousWasConverted = false;
            }

            if (currentClusterEvents.isEmpty()) {
                currentTrafficSource = extractTrafficSource(evt);
            }
            if (detectedRoute != null) {
                currentTargetRoute = detectedRoute;
            }
            if (isConversionEvent) {
                previousWasConverted = true;
            }

            currentClusterEvents.add(evt);
            if (evtTime != null) {
                lastEventTime = evtTime;
            }
        }

        if (!currentClusterEvents.isEmpty()) {
            clusters.add(buildCluster(clusters.size() + 1, currentClusterEvents, currentTargetRoute, currentTrafficSource, isRegistered));
        }

        return clusters;
    }

    private CustomerJourneyTimelineDto.JourneyClusterDto buildCluster(
            int index, List<CustomerJourneyTimelineDto.JourneyEventDto> events,
            String targetRoute, String trafficSource, boolean isRegistered) {

        String startTime = events.get(0).getTimestamp();
        String endTime = events.get(events.size() - 1).getTimestamp();

        int totalActive = events.stream().mapToInt(e -> e.getActiveSeconds() != null ? e.getActiveSeconds() : 0).sum();
        int totalIdle = events.stream().mapToInt(e -> e.getIdleSeconds() != null ? e.getIdleSeconds() : 0).sum();

        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        int durationMinutes = 1;
        if (start != null && end != null) {
            durationMinutes = Math.max(1, (int) java.time.Duration.between(start, end).toMinutes());
        }

        boolean hasConverted = events.stream().anyMatch(this::isConversion);
        LocalDateTime now = LocalDateTime.now();
        boolean isRecent = (end != null && end.isAfter(now.minusMinutes(45)));

        String status;
        if (hasConverted) {
            status = "CONVERTED";
        } else if (isRecent) {
            status = "IN_PROGRESS";
        } else {
            status = "ABANDONED";
        }

        String routeDisplay = targetRoute != null ? targetRoute : "Toàn bộ hệ thống";
        String statusText = status.equals("CONVERTED") ? "Chuyển đổi thành công" : (status.equals("ABANDONED") ? "Bỏ rơi giỏ hàng" : "Đang tìm hiểu");
        String title = "Hành trình #" + index + ": " + (targetRoute != null ? "Tuyến " + targetRoute : "Khám phá dịch vụ") + " (" + statusText + ")";

        return CustomerJourneyTimelineDto.JourneyClusterDto.builder()
                .journeyId("J-" + index)
                .title(title)
                .status(status)
                .targetRoute(routeDisplay)
                .trafficSource(trafficSource != null ? trafficSource : "direct")
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .activeSeconds(totalActive)
                .idleSeconds(totalIdle)
                .customerIdentified(isRegistered)
                .events(events)
                .build();
    }

    private String extractRouteFromEvent(CustomerJourneyTimelineDto.JourneyEventDto evt) {
        String text = (evt.getTitle() + " " + evt.getDescription() + " " + evt.getMetadata()).toLowerCase();
        if (text.contains("đà lạt") || text.contains("da lat")) return "TP.HCM ⇄ Đà Lạt";
        if (text.contains("nha trang")) return "TP.HCM ⇄ Nha Trang";
        if (text.contains("cần thơ") || text.contains("can tho")) return "TP.HCM ⇄ Cần Thơ";
        if (text.contains("vũng tàu") || text.contains("vung tau")) return "TP.HCM ⇄ Vũng Tàu";
        if (text.contains("đà nẵng") || text.contains("da nang")) return "Đà Nẵng ⇄ Hội An";
        if (text.contains("hà nội") || text.contains("hạ long")) return "Hà Nội ⇄ Hạ Long";
        return null;
    }

    private boolean isConversion(CustomerJourneyTimelineDto.JourneyEventDto evt) {
        String text = (evt.getTitle() + " " + evt.getDescription() + " " + evt.getMetadata()).toUpperCase();
        return text.contains("BOOKING_SUCCESS") || text.contains("PAYMENT_SUCCESS") 
                || text.contains("PAYMENT_PAID") || text.contains("CHECKOUT_COMPLETE");
    }

    private String extractTrafficSource(CustomerJourneyTimelineDto.JourneyEventDto evt) {
        if (evt.getMetadata() != null && evt.getMetadata().contains("TrafficSource: ")) {
            String sub = evt.getMetadata().substring(evt.getMetadata().indexOf("TrafficSource: ") + 15);
            int end = sub.indexOf(" |");
            return end != -1 ? sub.substring(0, end) : sub;
        }
        return "direct";
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(timeStr, ISO_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> getRecentSessions(int limit) {
        return sessionRepository.findRecentSessions(PageRequest.of(0, Math.min(limit, 100)));
    }

    @Override
    @Transactional(readOnly = true)
    public LiveVisitorsSummaryResponse getLiveVisitors(int activeWindowMinutes) {
        int window = activeWindowMinutes > 0 ? activeWindowMinutes : 5;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(window);

        List<UserSession> activeSessions = sessionRepository.findActiveSessionsSince(threshold);
        // Fallback to recent sessions if local dev has low live volume
        if (activeSessions.isEmpty()) {
            activeSessions = sessionRepository.findRecentSessions(PageRequest.of(0, 10));
        }

        List<LiveVisitorDto> visitorDtos = new ArrayList<>();
        int registeredCount = 0;
        int guestCount = 0;

        for (UserSession s : activeSessions) {
            boolean isRegistered = false;
            String name = "Khách vãng lai";
            String email = "Chưa đăng nhập";

            if (s.getCustomerId() != null) {
                Optional<Customer> c = customerRepository.findById(s.getCustomerId());
                if (c.isPresent()) {
                    isRegistered = true;
                    name = c.get().getFullName();
                    email = c.get().getEmail();
                }
            }

            if (isRegistered) {
                registeredCount++;
            } else {
                guestCount++;
            }

            // Detect Device
            String ua = s.getUserAgent() != null ? s.getUserAgent() : "Unknown";
            String deviceType = (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone"))
                    ? "Mobile" : "Desktop";

            // Current Page & Dwell
            List<PageView> pvs = pageViewRepository.findBySessionIdOrderByEnteredAtAsc(s.getId());
            PageView lastPv = pvs.isEmpty() ? null : pvs.get(pvs.size() - 1);
            String pageUrl = lastPv != null ? lastPv.getPageUrl() : "/";
            String pageTitle = lastPv != null ? lastPv.getPageTitle() : "Trang chủ";
            int activeSec = (lastPv != null && lastPv.getActiveDurationSeconds() != null) ? lastPv.getActiveDurationSeconds() : 0;
            int idleSec = (lastPv != null && lastPv.getIdleDurationSeconds() != null) ? lastPv.getIdleDurationSeconds() : 0;

            // Last Action
            List<UserAction> acts = actionRepository.findBySessionIdOrderByCreatedAtAsc(s.getId());
            UserAction lastAct = acts.isEmpty() ? null : acts.get(acts.size() - 1);
            String lastActType = lastAct != null ? lastAct.getActionType() : "ĐANG_XEM_TRANG";
            String lastActPayload = lastAct != null ? lastAct.getPayload() : "";
            String lastActivity = (lastAct != null && lastAct.getCreatedAt() != null)
                    ? lastAct.getCreatedAt().format(ISO_FORMATTER)
                    : (s.getEndTime() != null ? s.getEndTime().format(ISO_FORMATTER) : "");

            // Recommendations
            List<UserRecommendation> recs;
            if (s.getCustomerId() != null) {
                recs = recommendationRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(s.getCustomerId());
            } else {
                recs = recommendationRepository.findTop5ByAnonymousIdOrderByCreatedAtDesc(s.getAnonymousId());
            }

            String persona = (!recs.isEmpty() && recs.get(0).getPersonaCluster() != null)
                    ? recs.get(0).getPersonaCluster() : "POPULAR_TRENDING";

            List<LiveVisitorDto.LiveRecommendedTripDto> tripDtos = new ArrayList<>();
            for (UserRecommendation r : recs) {
                if (r.getRecommendedTripId() != null) {
                    tripRepository.findById(r.getRecommendedTripId()).ifPresent(t -> {
                        String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                        String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                        String vType = t.getVehicle() != null ? t.getVehicle().getType() : "Limousine";
                        String price = t.getPrice() != null ? java.text.NumberFormat.getInstance(new Locale("vi", "VN")).format(t.getPrice()) + " đ" : "N/A";
                        String time = t.getDepartureTime() != null ? t.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM")) : "";

                        tripDtos.add(LiveVisitorDto.LiveRecommendedTripDto.builder()
                                .tripId(t.getId())
                                .route(dep + " ➔ " + arr)
                                .departureTime(time)
                                .price(price)
                                .vehicleType(vType)
                                .build());
                    });
                }
            }

            visitorDtos.add(LiveVisitorDto.builder()
                    .sessionId(s.getId().toString())
                    .anonymousId(s.getAnonymousId())
                    .customerId(s.getCustomerId() != null ? s.getCustomerId().toString() : null)
                    .isRegisteredCustomer(isRegistered)
                    .customerName(name)
                    .customerEmail(email)
                    .ipAddress(s.getIpAddress() != null ? s.getIpAddress() : "127.0.0.1")
                    .userAgent(ua)
                    .deviceType(deviceType)
                    .trafficSource(s.getTrafficSource())
                    .utmCampaign(s.getUtmCampaign())
                    .currentPageUrl(pageUrl)
                    .currentPageTitle(pageTitle)
                    .activeSecondsOnPage(activeSec)
                    .idleSecondsOnPage(idleSec)
                    .lastActivityTime(lastActivity)
                    .lastActionType(lastActType)
                    .lastActionPayload(lastActPayload)
                    .matchedPersona(persona)
                    .recommendedTrips(tripDtos)
                    .build());
        }

        return LiveVisitorsSummaryResponse.builder()
                .totalOnline(visitorDtos.size())
                .registeredOnline(registeredCount)
                .guestOnline(guestCount)
                .checkedAt(LocalDateTime.now().format(ISO_FORMATTER))
                .visitors(visitorDtos)
                .build();
    }
}
