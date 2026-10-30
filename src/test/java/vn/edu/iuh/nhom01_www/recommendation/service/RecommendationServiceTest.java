package vn.edu.iuh.nhom01_www.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.iuh.nhom01_www.recommendation.model.PersonaType;
import vn.edu.iuh.nhom01_www.recommendation.service.impl.RecommendationServiceImpl;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.tracking.dto.RecommendedTripDto;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserRecommendationRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PageViewRepository pageViewRepository;

    @Mock
    private UserActionRepository actionRepository;

    @Mock
    private UserRecommendationRepository recommendationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripBookingAvailabilityService tripBookingAvailabilityService;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Trip sampleTrip;

    @BeforeEach
    void setUp() {
        Route route = Route.builder()
                .id(1L)
                .departureLocation("TP.HCM")
                .arrivalLocation("Đà Lạt")
                .basePrice(BigDecimal.valueOf(250000))
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .type("Limousine 34 phòng")
                .licensePlate("51B-999.88")
                .build();

        sampleTrip = Trip.builder()
                .id(1L)
                .route(route)
                .vehicle(vehicle)
                .departureTime(LocalDateTime.now().plusDays(1).withHour(22))
                .price(BigDecimal.valueOf(280000))
                .status("SCHEDULED")
                .build();
    }

    @Test
    @DisplayName("Should infer DALAT_LIMOUSINE persona when session viewed Dalat routes")
    void shouldInferDalatPersona() {
        UUID sessionId = UUID.randomUUID();
        UserSession session = UserSession.builder()
                .id(sessionId)
                .anonymousId("anon-dalat-user")
                .utmCampaign("tet-dalat-2026")
                .build();

        PageView pv = PageView.builder()
                .pageUrl("/search?to=Đà+Lạt")
                .pageTitle("Vé xe Limousine Đà Lạt")
                .build();

        when(sessionRepository.findByAnonymousIdOrCustomerIdOrderByCreatedAtDesc("anon-dalat-user", null))
                .thenReturn(List.of(session));
        when(pageViewRepository.findBySessionIdOrderByEnteredAtAsc(sessionId))
                .thenReturn(List.of(pv));
        when(actionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .thenReturn(Collections.emptyList());

        PersonaType persona = recommendationService.inferUserPersona("anon-dalat-user", null, null);

        assertThat(persona).isEqualTo(PersonaType.DALAT_LIMOUSINE);
    }

    @Test
    @DisplayName("Should generate personalized recommendations combining hybrid score and badges")
    void shouldGenerateRecommendations() {
        when(sessionRepository.findByAnonymousIdOrCustomerIdOrderByCreatedAtDesc("anon-test", null))
                .thenReturn(Collections.emptyList()); // Fallback to POPULAR_TRENDING
        when(tripRepository.findAll()).thenReturn(List.of(sampleTrip));
        when(tripBookingAvailabilityService.getAvailableSeatCount(sampleTrip)).thenReturn(24);

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                "anon-test", null, "127.0.0.1", 5
        );

        assertThat(results).isNotEmpty();
        RecommendedTripDto rec = results.get(0);
        assertThat(rec.getTripId()).isEqualTo(1L);
        assertThat(rec.getArrivalLocation()).isEqualTo("Đà Lạt");
        assertThat(rec.getMatchScore()).isGreaterThan(0.70);
        assertThat(rec.getBadgeText()).isNotBlank();
        assertThat(rec.getReason()).contains("Đà Lạt");
    }
}
