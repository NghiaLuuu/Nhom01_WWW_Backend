package vn.edu.iuh.nhom01_www.tracking.service;

import vn.edu.iuh.nhom01_www.tracking.dto.*;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;

import java.util.List;
import java.util.UUID;

public interface TrackingService {

    UserSession getOrCreateSession(TrackSessionRequest request, String ipAddress, String userAgent);

    PageView recordPageView(TrackPageViewRequest request);

    void updateHeartbeat(TrackHeartbeatRequest request);

    UserAction recordAction(TrackActionRequest request);

    void mergeCustomerSession(UUID sessionId, UUID customerId);

    TrackingAnalyticsResponse getAnalyticsOverview();

    CustomerJourneyTimelineDto getCustomerJourney(String identifier);

    List<UserSession> getRecentSessions(int limit);

    LiveVisitorsSummaryResponse getLiveVisitors(int activeWindowMinutes);

    LiveVisitorDetailDto getLiveVisitorDetail(UUID sessionId);
}

