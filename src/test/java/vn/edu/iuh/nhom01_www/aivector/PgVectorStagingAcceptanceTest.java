package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in staging acceptance test. It reads restored Route, Trip, and Vehicle rows and
 * uses deterministic lexical embeddings; it is not evidence of real-model semantic quality.
 */
@EnabledIfSystemProperty(named = "ai.vector.acceptance.enabled", matches = "true")
@EnabledIfSystemProperty(named = "ai.vector.acceptance.reset", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgVectorStagingAcceptanceTest {

    private JdbcTemplate jdbcTemplate;
    private VectorIngestionService ingestionService;
    private VectorSearchService searchService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                requiredProperty("ai.vector.it.jdbc-url"),
                requiredProperty("ai.vector.it.username"),
                requiredProperty("ai.vector.it.password"));
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM vector_ingestion_manifest");
        jdbcTemplate.update("DELETE FROM vector_collection_metadata");

        VietnameseLexicalEmbeddingModel embeddingModel = new VietnameseLexicalEmbeddingModel();
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .build();
        VectorDocumentMapper mapper = new VectorDocumentMapper();
        VectorEmbeddingProperties embeddingProperties = new VectorEmbeddingProperties();
        embeddingProperties.setCollectionName("vector_store");
        embeddingProperties.setProvider("test");
        embeddingProperties.setModel("vietnamese-lexical-embedding");
        embeddingProperties.setDimensions(1536);
        embeddingProperties.setConfigurationVersion("staging-acceptance-v1");
        VectorCollectionGuard collectionGuard = new JdbcVectorCollectionGuard(
                new JdbcVectorCollectionMetadataStore(jdbcTemplate), embeddingProperties, embeddingModel);
        ingestionService = new VectorIngestionService(
                repository(RouteRepository.class, loadRoutes()),
                repository(TripRepository.class, loadTrips()),
                repository(VehicleRepository.class, loadVehicles()),
                mapper,
                vectorStore,
                new JdbcVectorIngestionStateStore(jdbcTemplate),
                collectionGuard);
        searchService = new PgVectorSearchService(vectorStore, collectionGuard);
    }

    @Test
    void ingestsActualStagingDataTwiceAndReturnsRankedVietnameseSearchResults() {
        int routes = jdbcTemplate.queryForObject("SELECT count(*) FROM routes", Integer.class);
        int trips = jdbcTemplate.queryForObject("SELECT count(*) FROM trips", Integer.class);
        int vehicles = jdbcTemplate.queryForObject("SELECT count(*) FROM vehicles", Integer.class);
        assertThat(List.of(routes, trips, vehicles)).containsExactly(10, 50, 20);

        VectorIngestionReport firstRun = ingestionService.ingestAll();
        int vectorsAfterFirstRun = count("vector_store");
        int manifestsAfterFirstRun = count("vector_ingestion_manifest");
        assertThat(firstRun.sourcesSucceeded()).isEqualTo(80);
        assertThat(vectorsAfterFirstRun).isEqualTo(80);
        assertThat(manifestsAfterFirstRun).isEqualTo(80);

        VectorIngestionReport secondRun = ingestionService.ingestAll();
        assertThat(secondRun.documentsWritten()).isZero();
        assertThat(count("vector_store")).isEqualTo(vectorsAfterFirstRun);
        assertThat(count("vector_ingestion_manifest")).isEqualTo(manifestsAfterFirstRun);

        List<String> queries = List.of(
                "Tôi muốn đi từ Sài Gòn đến Đà Lạt.",
                "Tìm xe giường nằm.",
                "Có tuyến nào từ TP.HCM đến Đà Lạt?",
                "Các tuyến xe đang hoạt động?");
        for (String query : queries) {
            List<VectorSearchResult> results = searchService.search(new VectorSearchRequest(query, 5, 0.1d, null, null));
            assertThat(results).isNotEmpty().hasSizeLessThanOrEqualTo(5);
            assertThat(results).allSatisfy(result -> {
                assertThat(result.content()).isNotBlank();
                assertThat(result.metadata()).containsKeys("sourceType", "sourceId", "documentVersion");
                assertThat(result.similarityScore()).isNotNull().isGreaterThanOrEqualTo(0.1d);
            });
            emit(query, results);
        }

        List<VectorSearchResult> routeOnly = searchService.search(new VectorSearchRequest(
                "Có tuyến xe nào từ TP.HCM đến Đà Lạt?", 3, 0.1d, "ROUTE", null));
        assertThat(routeOnly).allSatisfy(result -> assertThat(result.metadata().get("sourceType")).isEqualTo("route"));
        List<VectorSearchResult> routeOne = searchService.search(new VectorSearchRequest(
                "Có tuyến xe nào từ TP.HCM đến Đà Lạt?", 20, 0.1d, null, 1L));
        assertThat(routeOne).allSatisfy(result -> assertThat(result.metadata().get("routeId")).isEqualTo("1"));
        assertThat(searchService.search(new VectorSearchRequest("Tôi muốn tìm xe đi Đà Lạt.", 20, 0.99d, null, null)))
                .allSatisfy(result -> assertThat(result.similarityScore()).isGreaterThanOrEqualTo(0.99d));
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName, Integer.class);
    }

    private List<Route> loadRoutes() {
        return jdbcTemplate.query("""
                SELECT id, departure_location, arrival_location, distance_km, duration_minutes, base_price, status
                FROM routes ORDER BY id
                """, (rs, rowNum) -> Route.builder().id(rs.getLong("id"))
                .departureLocation(rs.getString("departure_location"))
                .arrivalLocation(rs.getString("arrival_location"))
                .distanceKm(rs.getBigDecimal("distance_km"))
                .durationMinutes((Integer) rs.getObject("duration_minutes"))
                .basePrice(rs.getBigDecimal("base_price"))
                .status((Boolean) rs.getObject("status")).build());
    }

    private List<Vehicle> loadVehicles() {
        return jdbcTemplate.query("SELECT id, license_plate, type, capacity, status FROM vehicles ORDER BY id",
                (rs, rowNum) -> Vehicle.builder().id(rs.getLong("id"))
                        .licensePlate(rs.getString("license_plate"))
                        .type(rs.getString("type"))
                        .capacity((Integer) rs.getObject("capacity"))
                        .status(rs.getString("status")).build());
    }

    private List<Trip> loadTrips() {
        return jdbcTemplate.query("""
                SELECT t.id AS trip_id, t.departure_time, t.price, t.status AS trip_status,
                       r.id AS route_id, r.departure_location, r.arrival_location,
                       v.id AS vehicle_id, v.type AS vehicle_type, v.capacity, v.status AS vehicle_status
                FROM trips t
                JOIN routes r ON r.id = t.route_id
                JOIN vehicles v ON v.id = t.vehicle_id
                ORDER BY t.id
                """, (rs, rowNum) -> {
            Route route = Route.builder().id(rs.getLong("route_id"))
                    .departureLocation(rs.getString("departure_location"))
                    .arrivalLocation(rs.getString("arrival_location")).build();
            Vehicle vehicle = Vehicle.builder().id(rs.getLong("vehicle_id"))
                    .type(rs.getString("vehicle_type"))
                    .capacity((Integer) rs.getObject("capacity"))
                    .status(rs.getString("vehicle_status")).build();
            Timestamp departure = rs.getTimestamp("departure_time");
            return Trip.builder().id(rs.getLong("trip_id")).route(route).vehicle(vehicle)
                    .departureTime(departure == null ? null : departure.toLocalDateTime())
                    .price(rs.getBigDecimal("price")).status(rs.getString("trip_status")).build();
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, List<?> records) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, arguments) -> {
            if (method.getName().equals("findAll") && method.getParameterCount() == 0) {
                return records;
            }
            throw new UnsupportedOperationException("Acceptance repository only supports findAll");
        });
    }

    private void emit(String query, List<VectorSearchResult> results) {
        System.out.println("ACCEPTANCE_QUERY=" + query);
        for (int index = 0; index < results.size(); index++) {
            VectorSearchResult result = results.get(index);
            System.out.printf("ACCEPTANCE_RESULT=%d|%s|%s|%.6f|%s%n", index + 1,
                    result.metadata().get("sourceType"), result.metadata().get("sourceId"),
                    result.similarityScore(), result.content().replace('\n', ' '));
        }
    }

    private String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing staging property: " + name);
        }
        return value;
    }

    private static final class VietnameseLexicalEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> results = new ArrayList<>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                results.add(new Embedding(vector(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(results);
        }

        @Override
        public float[] embed(Document document) {
            return vector(document.getText());
        }

        @Override
        public int dimensions() {
            return 1536;
        }

        private static float[] vector(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            float[] vector = new float[1536];
            add(vector, 0, normalized, "đà lạt");
            add(vector, 1, normalized, "tp.hcm");
            add(vector, 2, normalized, "giường nằm");
            add(vector, 3, normalized, "ghế ngồi");
            add(vector, 4, normalized, "limousine");
            add(vector, 5, normalized, "loại xe");
            add(vector, 6, normalized, "đang hoạt động");
            add(vector, 7, normalized, "tuyến");
            if (vector[0] == 0f && vector[1] == 0f && vector[2] == 0f && vector[3] == 0f
                    && vector[4] == 0f && vector[5] == 0f && vector[6] == 0f && vector[7] == 0f) {
                vector[8] = 1f;
            }
            return vector;
        }

        private static void add(float[] vector, int index, String text, String token) {
            if (text.contains(token)) {
                vector[index] = 1f;
            }
        }
    }
}
