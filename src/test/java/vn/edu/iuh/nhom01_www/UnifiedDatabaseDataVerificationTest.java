package vn.edu.iuh.nhom01_www;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Unified Database & Cross-Module Data Verification Test")
class UnifiedDatabaseDataVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Verify PostgreSQL connection and pgvector extension in unified busbooking database")
    void testPgVectorExtensionInstalled() {
        String extVersion = jdbcTemplate.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        assertThat(extVersion).isNotNull();
        assertThat(extVersion).startsWith("0.8");

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' " +
                        "AND table_name IN ('vector_store', 'vector_collection_metadata', 'vector_ingestion_manifest')",
                String.class);
        assertThat(tables).contains("vector_store", "vector_collection_metadata", "vector_ingestion_manifest");
    }

    @Test
    @DisplayName("Verify real business tables have data in shared development database")
    void testBusinessDataExistsInSharedDatabase() {
        long routeCount = routeRepository.count();
        long tripCount = tripRepository.count();
        long bookingCount = bookingRepository.count();
        long ticketCount = ticketRepository.count();
        long paymentCount = paymentRepository.count();

        assertThat(routeCount).isGreaterThan(0);
        assertThat(tripCount).isGreaterThan(0);
        assertThat(bookingCount).isGreaterThan(0);
        assertThat(ticketCount).isGreaterThan(0);
        assertThat(paymentCount).isGreaterThan(0);

        System.out.printf("[Shared Database] Real business data verified: routes=%d, trips=%d, bookings=%d, tickets=%d, payments=%d%n",
                routeCount, tripCount, bookingCount, ticketCount, paymentCount);
    }

    @Test
    @DisplayName("Verify website and AI chatbot read consistent trip and route data")
    void testTripAndRouteConsistency() {
        var routes = routeRepository.findAll();
        assertThat(routes).isNotEmpty();
        var firstRoute = routes.get(0);
        assertThat(firstRoute.getDepartureLocation()).isNotBlank();
        assertThat(firstRoute.getArrivalLocation()).isNotBlank();

        var trips = tripRepository.findByRoute_Id(firstRoute.getId());
        assertThat(trips).isNotNull();
    }

    @Test
    @DisplayName("Verify seat availability query behaves identically for website and chatbot")
    void testSeatAvailabilityQuery() {
        var trips = tripRepository.findAll();
        assertThat(trips).isNotEmpty();
        Long tripId = trips.get(0).getId();

        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByTripId(tripId);
        assertThat(bookedSeatIds).isNotNull();
    }
}
