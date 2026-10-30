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
            // If last activity is within 30 minutes, reuse session
            LocalDateTime lastActive = session.getEndTime() != null ? session.getEndTime() : session.getStartTime();
            if (lastActive != null && lastActive.isAfter(now.minusMinutes(30))) {
                session.setEndTime(now);
                if (customerId != null && session.getCustomerId() == null) {
                    session.setCustomerId(customerId);
                }
                return sessionRepository.save(session);
            }
        }

        // Otherwise create new session
        UserSession newSession = UserSession.builder()
                .anonymousId(anonId)
                .customerId(customerId)
                .ipAddress(ipAddress)
                .userAgent(userAgent != null ? userAgent : (request != null ? request.getUserAgent() : "Unknown"))
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
                int addActive = request.getAdditionalActiveSeconds() != null ? Math.max(0, request.getAdditionalActiveSeconds()) : 0;
                int currentTotal = session.getTotalDurationSeconds() != null ? session.getTotalDurationSeconds() : 0;
                session.setTotalDurationSeconds(currentTotal + addActive);
                sessionRepository.save(session);
            });
        }

        if (request.getPageViewId() != null) {
            pageViewRepository.findById(request.getPageViewId()).ifPresent(pv -> {
                int addActive = request.getAdditionalActiveSeconds() != null ? Math.max(0, request.getAdditionalActiveSeconds()) : 0;
                int addIdle = request.getAdditionalIdleSeconds() != null ? Math.max(0, request.getAdditionalIdleSeconds()) : 0;

                int curActive = pv.getActiveDurationSeconds() != null ? pv.getActiveDurationSeconds() : 0;
                int curIdle = pv.getIdleDurationSeconds() != null ? pv.getIdleDurationSeconds() : 0;

                pv.setActiveDurationSeconds(curActive + addActive);
                pv.setIdleDurationSeconds(curIdle + addIdle);
                pv.setLeftAt(now);
                pageViewRepository.save(pv);
            });
        }
    }

    @Override
    @Transactional
    public UserAction recordAction(TrackActionRequest request) {
        if (request == null || request.getSessionId() == null) {
            throw new IllegalArgumentException("Session ID is required");
        }

        UserSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        PageView pageView = null;
        if (request.getPageViewId() != null) {
            pageView = pageViewRepository.findById(request.getPageViewId()).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        sessionRepository.save(session);

        UserAction action = UserAction.builder()
                .session(session)
                .pageView(pageView)
                .actionType(request.getActionType() != null ? request.getActionType().toUpperCase() : "GENERAL_CLICK")
                .payload(request.getPayload())
                .build();

        return actionRepository.save(action);
    }

    @Override
    @Transactional
    public void mergeCustomerSession(UUID sessionId, UUID customerId) {
        if (sessionId == null || customerId == null) return;
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCustomerId(customerId);
            sessionRepository.save(session);
            log.info("[Tracking] Merged guest session {} with customer {}", sessionId, customerId);
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
            return CustomerJourneyTimelineDto.builder().identifier("Unknown").events(Collections.emptyList()).build();
        }

        List<UserSession> sessions;
        UUID parsedUuid = null;
        try {
            parsedUuid = UUID.fromString(identifier);
        } catch (IllegalArgumentException ignored) {}

        if (parsedUuid != null) {
            // Check if it's a customerId or sessionId
            final UUID finalParsed = parsedUuid;
            sessions = sessionRepository.findByCustomerIdOrderByCreatedAtDesc(finalParsed);
            if (sessions.isEmpty()) {
                sessionRepository.findById(finalParsed).ifPresent(sessions::add);
            }
        } else if (identifier.startsWith("anon-")) {
            sessions = sessionRepository.findByAnonymousIdOrderByCreatedAtDesc(identifier);
        } else {
            sessions = sessionRepository.findByIpAddressOrderByCreatedAtDesc(identifier);
        }

        String customerName = "Khách vãng lai";
        String customerEmail = "N/A";
        String matchedPersona = "Chưa xác định";

        if (!sessions.isEmpty() && sessions.get(0).getCustomerId() != null) {
            Optional<Customer> customer = customerRepository.findById(sessions.get(0).getCustomerId());
            if (customer.isPresent()) {
                customerName = customer.get().getFullName();
                customerEmail = customer.get().getEmail();
            }
        }

        // Get recommendations to check persona
        List<UserRecommendation> recs;
        if (!sessions.isEmpty() && sessions.get(0).getCustomerId() != null) {
            recs = recommendationRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(sessions.get(0).getCustomerId());
        } else {
            recs = recommendationRepository.findTop5ByAnonymousIdOrderByCreatedAtDesc(identifier);
        }
        if (!recs.isEmpty() && recs.get(0).getPersonaCluster() != null) {
            matchedPersona = recs.get(0).getPersonaCluster();
        }

        List<CustomerJourneyTimelineDto.JourneyEventDto> events = new ArrayList<>();
        int totalPageViews = 0;
        int totalActions = 0;

        for (UserSession session : sessions) {
            events.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                    .id("sess-" + session.getId())
                    .type("SESSION_START")
                    .title("Bắt đầu phiên truy cập")
                    .description("Nguồn: " + session.getTrafficSource() + " | IP: " + session.getIpAddress())
                    .timestamp(session.getStartTime() != null ? session.getStartTime().format(ISO_FORMATTER) : "")
                    .activeSeconds(session.getTotalDurationSeconds())
                    .idleSeconds(0)
                    .metadata("Campaign: " + (session.getUtmCampaign() != null ? session.getUtmCampaign() : "None"))
                    .build());

            List<PageView> pvs = pageViewRepository.findBySessionIdOrderByEnteredAtAsc(session.getId());
            totalPageViews += pvs.size();
            for (PageView pv : pvs) {
                events.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                        .id("pv-" + pv.getId())
                        .type("PAGE_VIEW")
                        .title("Xem trang: " + pv.getPageTitle())
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
                events.add(CustomerJourneyTimelineDto.JourneyEventDto.builder()
                        .id("act-" + act.getId())
                        .type("ACTION")
                        .title("Hành động: " + act.getActionType())
                        .description("Chi tiết: " + (act.getPayload() != null ? act.getPayload() : ""))
                        .timestamp(act.getCreatedAt() != null ? act.getCreatedAt().format(ISO_FORMATTER) : "")
                        .metadata("ActionType: " + act.getActionType())
                        .build());
            }
        }

        return CustomerJourneyTimelineDto.builder()
                .identifier(identifier)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .totalSessions(sessions.size())
                .totalPageViews(totalPageViews)
                .totalActions(totalActions)
                .matchedPersona(matchedPersona)
                .events(events)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSession> getRecentSessions(int limit) {
        return sessionRepository.findRecentSessions(PageRequest.of(0, Math.min(limit, 100)));
    }
}
