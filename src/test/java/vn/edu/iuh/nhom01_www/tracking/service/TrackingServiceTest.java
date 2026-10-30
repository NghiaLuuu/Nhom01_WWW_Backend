package vn.edu.iuh.nhom01_www.tracking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackActionRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackHeartbeatRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackPageViewRequest;
import vn.edu.iuh.nhom01_www.tracking.dto.TrackSessionRequest;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserRecommendationRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;
import vn.edu.iuh.nhom01_www.tracking.service.impl.TrackingServiceImpl;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PageViewRepository pageViewRepository;

    @Mock
    private UserActionRepository actionRepository;

    @Mock
    private UserRecommendationRepository recommendationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    private UUID sessionId;
    private UserSession sampleSession;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        sampleSession = UserSession.builder()
                .id(sessionId)
                .anonymousId("anon-test-123")
                .trafficSource("facebook")
                .utmCampaign("tet-2026")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .totalDurationSeconds(0)
                .build();
    }

    @Test
    @DisplayName("Should create new session if none exists")
    void shouldCreateNewSessionWhenNoneExists() {
        when(sessionRepository.findTopByAnonymousIdOrderByCreatedAtDesc("anon-test-123"))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrackSessionRequest req = TrackSessionRequest.builder()
                .anonymousId("anon-test-123")
                .trafficSource("facebook")
                .utmCampaign("tet-2026")
                .build();

        UserSession result = trackingService.getOrCreateSession(req, "192.168.1.100", "Mozilla/5.0");

        assertThat(result).isNotNull();
        assertThat(result.getAnonymousId()).isEqualTo("anon-test-123");
        assertThat(result.getTrafficSource()).isEqualTo("facebook");
        assertThat(result.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should record pageview linked to session")
    void shouldRecordPageView() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any())).thenReturn(sampleSession);
        when(pageViewRepository.save(any(PageView.class))).thenAnswer(inv -> {
            PageView pv = inv.getArgument(0);
            pv.setId(UUID.randomUUID());
            return pv;
        });

        TrackPageViewRequest req = TrackPageViewRequest.builder()
                .sessionId(sessionId)
                .pageUrl("/trips")
                .pageTitle("Danh sách chuyến xe")
                .previousPageUrl("/")
                .build();

        PageView result = trackingService.recordPageView(req);

        assertThat(result).isNotNull();
        assertThat(result.getPageUrl()).isEqualTo("/trips");
        assertThat(result.getPageTitle()).isEqualTo("Danh sách chuyến xe");
        assertThat(result.getSession().getId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Should update heartbeat active and idle seconds")
    void shouldUpdateHeartbeat() {
        UUID pageViewId = UUID.randomUUID();
        PageView pv = PageView.builder()
                .id(pageViewId)
                .session(sampleSession)
                .pageUrl("/trips")
                .activeDurationSeconds(10)
                .idleDurationSeconds(5)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));
        when(pageViewRepository.findById(pageViewId)).thenReturn(Optional.of(pv));

        TrackHeartbeatRequest req = TrackHeartbeatRequest.builder()
                .sessionId(sessionId)
                .pageViewId(pageViewId)
                .additionalActiveSeconds(15)
                .additionalIdleSeconds(10)
                .build();

        trackingService.updateHeartbeat(req);

        verify(sessionRepository).save(sampleSession);
        verify(pageViewRepository).save(pv);
        assertThat(pv.getActiveDurationSeconds()).isEqualTo(25);
        assertThat(pv.getIdleDurationSeconds()).isEqualTo(15);
        assertThat(sampleSession.getTotalDurationSeconds()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should record user action")
    void shouldRecordUserAction() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any())).thenReturn(sampleSession);
        when(actionRepository.save(any(UserAction.class))).thenAnswer(inv -> {
            UserAction act = inv.getArgument(0);
            act.setId(UUID.randomUUID());
            return act;
        });

        TrackActionRequest req = TrackActionRequest.builder()
                .sessionId(sessionId)
                .actionType("SEARCH")
                .payload("{\"from\":\"TP.HCM\",\"to\":\"Đà Lạt\"}")
                .build();

        UserAction result = trackingService.recordAction(req);

        assertThat(result).isNotNull();
        assertThat(result.getActionType()).isEqualTo("SEARCH");
        assertThat(result.getPayload()).contains("Đà Lạt");
    }

    @Test
    @DisplayName("Should merge customer session on login")
    void shouldMergeCustomerSession() {
        UUID customerId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(sampleSession));

        trackingService.mergeCustomerSession(sessionId, customerId);

        verify(sessionRepository).save(sampleSession);
        assertThat(sampleSession.getCustomerId()).isEqualTo(customerId);
    }
}

