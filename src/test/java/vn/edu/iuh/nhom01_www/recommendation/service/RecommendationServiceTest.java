package vn.edu.iuh.nhom01_www.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
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
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private BookingRepository bookingRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripBookingAvailabilityService tripBookingAvailabilityService;

    private RecommendationServiceImpl recommendationService;

    private Trip sampleTrip;
    private Trip tripHcmToPhuYen;
    private Trip tripPhuYenToHcm;
    private Trip tripDaNangToPhuYen;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                sessionRepository,
                pageViewRepository,
                actionRepository,
                recommendationRepository,
                bookingRepository,
                tripRepository,
                tripBookingAvailabilityService,
                Optional.empty(),
                Optional.empty()
        );

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

        Route rHcmPy = Route.builder().id(11L).departureLocation("TP.HCM").arrivalLocation("Phú Yên").build();
        Route rPyHcm = Route.builder().id(12L).departureLocation("Phú Yên").arrivalLocation("TP.HCM").build();
        Route rDnPy = Route.builder().id(13L).departureLocation("Đà Nẵng").arrivalLocation("Phú Yên").build();

        tripHcmToPhuYen = Trip.builder().id(101L).route(rHcmPy).price(BigDecimal.valueOf(280000)).departureTime(LocalDateTime.now().plusDays(1).withHour(19)).build();
        tripPhuYenToHcm = Trip.builder().id(102L).route(rPyHcm).price(BigDecimal.valueOf(280000)).departureTime(LocalDateTime.now().plusDays(2).withHour(19)).build();
        tripDaNangToPhuYen = Trip.builder().id(103L).route(rDnPy).price(BigDecimal.valueOf(260000)).departureTime(LocalDateTime.now().plusDays(1).withHour(8)).build();
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

        when(sessionRepository.findByAnonymousIdOrderByCreatedAtDesc("anon-dalat-user"))
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
        when(sessionRepository.findByAnonymousIdOrderByCreatedAtDesc("anon-test"))
                .thenReturn(Collections.emptyList());
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

    @Test
    @DisplayName("Should prioritize trips departing from user's current city with nearby badge")
    void shouldPrioritizeTripsFromCurrentCity() {
        Route routeHn = Route.builder().id(2L).departureLocation("Hà Nội").arrivalLocation("Hạ Long").build();
        Trip tripHn = Trip.builder().id(2L).route(routeHn).price(BigDecimal.valueOf(200000)).departureTime(LocalDateTime.now().plusDays(1)).build();

        when(sessionRepository.findByAnonymousIdOrderByCreatedAtDesc("anon-loc"))
                .thenReturn(Collections.emptyList());
        when(tripRepository.findAll()).thenReturn(List.of(tripHn, sampleTrip));

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                "anon-loc", null, "127.0.0.1", "TP.HCM", 5
        );

        assertThat(results).isNotEmpty();
        RecommendedTripDto topTrip = results.get(0);
        assertThat(topTrip.getDepartureLocation()).isEqualTo("TP.HCM");
        assertThat(topTrip.getBadgeText()).contains("Khởi hành gần bạn");
    }

    @Test
    @DisplayName("Commuter habit without location: should recommend bidirectional corridor trips")
    void shouldRecommendBidirectionalForCommuterWithoutLocation() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("commuter@vexe.vn").build();

        Booking b1 = Booking.builder().customer(customer).trip(tripHcmToPhuYen).status("PAID").build();
        Booking b2 = Booking.builder().customer(customer).trip(tripPhuYenToHcm).status("PAID").build();

        when(bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(b1, b2));
        when(tripRepository.findAll()).thenReturn(List.of(sampleTrip, tripHcmToPhuYen, tripPhuYenToHcm));

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                null, customerId, "127.0.0.1", null, 5
        );

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        RecommendedTripDto top1 = results.get(0);
        assertThat(top1.getBadgeText()).contains("Tuyến đường quen thuộc");
        assertThat(top1.getMatchScore()).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    @DisplayName("Commuter habit at Base A (TP.HCM): should prioritize TP.HCM -> Phú Yên as #1 with Về quê badge")
    void shouldPrioritizeTripHomeWhenAtBaseA() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("commuter@vexe.vn").build();

        Booking b1 = Booking.builder().customer(customer).trip(tripHcmToPhuYen).status("PAID").build();
        Booking b2 = Booking.builder().customer(customer).trip(tripPhuYenToHcm).status("PAID").build();

        when(bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(b1, b2));
        when(tripRepository.findAll()).thenReturn(List.of(sampleTrip, tripPhuYenToHcm, tripHcmToPhuYen));

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                null, customerId, "127.0.0.1", "TP.HCM", 5
        );

        assertThat(results).isNotEmpty();
        RecommendedTripDto top1 = results.get(0);
        assertThat(top1.getDepartureLocation()).isEqualTo("TP.HCM");
        assertThat(top1.getArrivalLocation()).isEqualTo("Phú Yên");
        assertThat(top1.getBadgeText()).contains("Về quê");
        assertThat(top1.getMatchScore()).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    @DisplayName("Commuter habit at Base B (Phú Yên): should prioritize Phú Yên -> TP.HCM as #1 with Quay lại làm việc badge")
    void shouldPrioritizeTripWorkWhenAtBaseB() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("commuter@vexe.vn").build();

        Booking b1 = Booking.builder().customer(customer).trip(tripHcmToPhuYen).status("PAID").build();
        Booking b2 = Booking.builder().customer(customer).trip(tripPhuYenToHcm).status("PAID").build();

        when(bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(b1, b2));
        when(tripRepository.findAll()).thenReturn(List.of(sampleTrip, tripHcmToPhuYen, tripPhuYenToHcm));

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                null, customerId, "127.0.0.1", "Phú Yên", 5
        );

        assertThat(results).isNotEmpty();
        RecommendedTripDto top1 = results.get(0);
        assertThat(top1.getDepartureLocation()).isEqualTo("Phú Yên");
        assertThat(top1.getArrivalLocation()).isEqualTo("TP.HCM");
        assertThat(top1.getBadgeText()).contains("Quay lại làm việc");
        assertThat(top1.getMatchScore()).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    @DisplayName("Commuter habit at 3rd city (Đà Nẵng): should prioritize trips returning to base corridor")
    void shouldPrioritizeTripToBaseWhenAtThirdCity() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).email("commuter@vexe.vn").build();

        Booking b1 = Booking.builder().customer(customer).trip(tripHcmToPhuYen).status("PAID").build();
        Booking b2 = Booking.builder().customer(customer).trip(tripPhuYenToHcm).status("PAID").build();

        when(bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(b1, b2));
        when(tripRepository.findAll()).thenReturn(List.of(sampleTrip, tripHcmToPhuYen, tripPhuYenToHcm, tripDaNangToPhuYen));

        List<RecommendedTripDto> results = recommendationService.getPersonalizedRecommendations(
                null, customerId, "127.0.0.1", "Đà Nẵng", 5
        );

        assertThat(results).isNotEmpty();
        RecommendedTripDto top1 = results.get(0);
        assertThat(top1.getDepartureLocation()).isEqualTo("Đà Nẵng");
        assertThat(top1.getArrivalLocation()).isEqualTo("Phú Yên");
        assertThat(top1.getBadgeText()).contains("Chuyến về căn cứ");
        assertThat(top1.getMatchScore()).isGreaterThanOrEqualTo(0.95);
    }
}
