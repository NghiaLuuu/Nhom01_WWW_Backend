package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in acceptance test against the real OpenAI embedding endpoint. It is deliberately
 * restricted to the isolated busbooking_openai_it database and never uses the local
 * busbooking port mapping. The test makes at most one ingestion embedding request for the
 * 80 source documents and five query embedding requests.
 */
@EnabledIfSystemProperty(named = "ai.vector.acceptance.enabled", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgVectorOpenAiStagingAcceptanceTest {

    private static final String ACCEPTANCE_JDBC_URL =
            "jdbc:postgresql://postgres_openai_it:5432/busbooking_openai_it";
    private static final String MODEL = "text-embedding-3-small";
    private static final int DIMENSIONS = 1536;

    private JdbcTemplate jdbcTemplate;
    private VectorIngestionService ingestionService;
    private VectorSearchService searchService;
    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        String apiKey = requiredEnvironment("OPENAI_API_KEY");
        String jdbcUrl = requiredProperty("ai.vector.it.jdbc-url");
        assertDedicatedAcceptanceDatabase(jdbcUrl);

        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(jdbcUrl,
                requiredProperty("ai.vector.it.username"), requiredProperty("ai.vector.it.password")));
        assertSourceDataset();
        resetDedicatedAcceptanceCollection();

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .options(OpenAiEmbeddingOptions.builder().apiKey(apiKey).model(MODEL).dimensions(DIMENSIONS).build())
                .build();
        assertThat(embeddingModel.dimensions()).isEqualTo(DIMENSIONS);

        vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(DIMENSIONS)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .build();
        VectorEmbeddingProperties properties = new VectorEmbeddingProperties();
        properties.setCollectionName("vector_store");
        properties.setProvider("openai");
        properties.setModel(MODEL);
        properties.setDimensions(DIMENSIONS);
        properties.setConfigurationVersion("v1");
        VectorCollectionGuard collectionGuard = new JdbcVectorCollectionGuard(
                new JdbcVectorCollectionMetadataStore(jdbcTemplate), properties, embeddingModel);
        ingestionService = new VectorIngestionService(
                repository(RouteRepository.class, loadRoutes()), repository(TripRepository.class, loadTrips()),
                repository(VehicleRepository.class, loadVehicles()), new VectorDocumentMapper(), vectorStore,
                new JdbcVectorIngestionStateStore(jdbcTemplate), collectionGuard);
        searchService = new PgVectorSearchService(vectorStore, collectionGuard);
    }

    @Test
    void ingestsRealOpenAiEmbeddingsAndValidatesVietnameseTopFiveSemanticSearch() {
        VectorIngestionReport firstRun = ingestionService.ingestAll();
        assertThat(firstRun.sourcesRead()).isEqualTo(80);
        assertThat(firstRun.sourcesSucceeded()).isEqualTo(80);
        assertThat(firstRun.sourcesFailed()).isZero();
        assertThat(firstRun.documentsWritten()).isEqualTo(80);
        assertThat(count("vector_store")).isEqualTo(80);
        assertThat(count("vector_ingestion_manifest")).isEqualTo(80);
        assertThat(jdbcTemplate.queryForObject("SELECT vector_dims(embedding) FROM vector_store LIMIT 1", Integer.class))
                .isEqualTo(DIMENSIONS);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT embedding_provider, embedding_model, embedding_dimensions, configuration_version
                FROM vector_collection_metadata WHERE collection_name = 'vector_store'
                """))
                .containsEntry("embedding_provider", "openai")
                .containsEntry("embedding_model", MODEL)
                .containsEntry("embedding_dimensions", DIMENSIONS)
                .containsEntry("configuration_version", "v1");

        VectorIngestionReport secondRun = ingestionService.ingestAll();
        assertThat(secondRun.documentsWritten()).isZero();
        assertThat(secondRun.sourcesSucceeded()).isZero();
        assertThat(count("vector_store")).isEqualTo(80);
        assertThat(count("vector_ingestion_manifest")).isEqualTo(80);

        assertDirectionalTopFiveImprovesOverUnfilteredSemanticBaseline();
        assertSemanticTopFive("Tìm xe giường nằm.", "Giường nằm");
        assertSemanticTopFive("Có tuyến nào từ Đà Nẵng đến Huế?", "Đà Nẵng", "Huế");
        assertSemanticTopFive("Tìm chuyến từ Sài Gòn đi Nha Trang.", "TP.HCM", "Nha Trang");
        assertSemanticTopFive("Tuyến xe nào đi từ Hà Nội đến Hải Phòng?", "Hà Nội", "Hải Phòng");
    }

    private void assertSemanticTopFive(String query, String... requiredTerms) {
        List<VectorSearchResult> results = searchService.search(new VectorSearchRequest(query, 5, 0.0d, null, null));
        assertThat(results).as("Top-5 results for: %s", query).isNotEmpty().hasSizeLessThanOrEqualTo(5);
        for (int index = 0; index < results.size(); index++) {
            VectorSearchResult result = results.get(index);
            System.out.printf("OPENAI_ACCEPTANCE_RESULT=%d|%.6f|%s|%s%n", index + 1,
                    result.similarityScore(), result.metadata().get("sourceType"), safeForLog(result.content()));
        }
        assertThat(results).anySatisfy(result -> {
            for (String requiredTerm : requiredTerms) {
                assertThat(result.content()).containsIgnoringCase(requiredTerm);
            }
        });
    }

    private void assertDirectionalTopFiveImprovesOverUnfilteredSemanticBaseline() {
        String query = "Tôi muốn đi từ Sài Gòn đến Đà Lạt.";
        List<Document> baseline = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(5)
                .similarityThreshold(0.0d).build());
        emitBaseline(query, baseline);

        List<VectorSearchResult> filtered = searchService.search(new VectorSearchRequest(query, 5, 0.0d, null, null));
        assertThat(filtered).as("Directional Top-5 results for: %s", query).isNotEmpty().hasSizeLessThanOrEqualTo(5);
        assertThat(filtered).allSatisfy(result -> assertThat(result.metadata())
                .containsEntry("departureLocationNormalized", "tp hcm")
                .containsEntry("arrivalLocationNormalized", "da lat"));
        assertThat(filtered.get(0).content()).containsIgnoringCase("TP.HCM").containsIgnoringCase("Đà Lạt");
        for (int index = 0; index < filtered.size(); index++) {
            VectorSearchResult result = filtered.get(index);
            System.out.printf("OPENAI_DIRECTIONAL_RESULT=%d|%.6f|%s|%s%n", index + 1,
                    result.similarityScore(), result.metadata().get("sourceType"), safeForLog(result.content()));
        }
    }

    private void emitBaseline(String query, List<Document> baseline) {
        System.out.println("OPENAI_UNFILTERED_BASELINE_QUERY=" + query);
        for (int index = 0; index < baseline.size(); index++) {
            Document document = baseline.get(index);
            System.out.printf("OPENAI_UNFILTERED_BASELINE=%d|%.6f|%s|%s%n", index + 1,
                    document.getScore(), document.getMetadata().get("sourceType"), safeForLog(document.getText()));
        }
    }

    private void assertDedicatedAcceptanceDatabase(String jdbcUrl) {
        if (!ACCEPTANCE_JDBC_URL.equals(jdbcUrl)) {
            throw new IllegalStateException("Acceptance test requires the dedicated busbooking_openai_it Docker datasource");
        }
    }

    private void assertSourceDataset() {
        assertThat(count("routes")).isEqualTo(10);
        assertThat(count("trips")).isEqualTo(50);
        assertThat(count("vehicles")).isEqualTo(20);
    }

    private void resetDedicatedAcceptanceCollection() {
        // This method is reachable only after assertDedicatedAcceptanceDatabase has accepted the exact,
        // isolated Docker JDBC URL. It never runs against localhost or an arbitrary staging/production database.
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM vector_ingestion_manifest");
        jdbcTemplate.update("DELETE FROM vector_collection_metadata");
        assertThat(count("vector_store")).isZero();
        assertThat(count("vector_ingestion_manifest")).isZero();
        assertThat(count("vector_collection_metadata")).isZero();
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private List<Route> loadRoutes() {
        return jdbcTemplate.query("""
                SELECT id, departure_location, arrival_location, distance_km, duration_minutes, base_price, status
                FROM routes ORDER BY id
                """, (rs, rowNum) -> Route.builder().id(rs.getLong("id"))
                .departureLocation(rs.getString("departure_location")).arrivalLocation(rs.getString("arrival_location"))
                .distanceKm(rs.getBigDecimal("distance_km")).durationMinutes((Integer) rs.getObject("duration_minutes"))
                .basePrice(rs.getBigDecimal("base_price")).status((Boolean) rs.getObject("status")).build());
    }

    private List<Vehicle> loadVehicles() {
        return jdbcTemplate.query("SELECT id, license_plate, type, capacity, status FROM vehicles ORDER BY id",
                (rs, rowNum) -> Vehicle.builder().id(rs.getLong("id")).licensePlate(rs.getString("license_plate"))
                        .type(rs.getString("type")).capacity((Integer) rs.getObject("capacity"))
                        .status(rs.getString("status")).build());
    }

    private List<Trip> loadTrips() {
        return jdbcTemplate.query("""
                SELECT t.id AS trip_id, t.departure_time, t.price, t.status AS trip_status,
                       r.id AS route_id, r.departure_location, r.arrival_location,
                       v.id AS vehicle_id, v.type AS vehicle_type, v.capacity, v.status AS vehicle_status
                FROM trips t JOIN routes r ON r.id = t.route_id JOIN vehicles v ON v.id = t.vehicle_id ORDER BY t.id
                """, (rs, rowNum) -> {
            Route route = Route.builder().id(rs.getLong("route_id")).departureLocation(rs.getString("departure_location"))
                    .arrivalLocation(rs.getString("arrival_location")).build();
            Vehicle vehicle = Vehicle.builder().id(rs.getLong("vehicle_id")).type(rs.getString("vehicle_type"))
                    .capacity((Integer) rs.getObject("capacity")).status(rs.getString("vehicle_status")).build();
            Timestamp departure = rs.getTimestamp("departure_time");
            return Trip.builder().id(rs.getLong("trip_id")).route(route).vehicle(vehicle)
                    .departureTime(departure == null ? null : departure.toLocalDateTime()).price(rs.getBigDecimal("price"))
                    .status(rs.getString("trip_status")).build();
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, List<?> rows) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, arguments) -> {
            if (method.getName().equals("findAll") && method.getParameterCount() == 0) return rows;
            throw new UnsupportedOperationException("Acceptance repository only supports findAll");
        });
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required in the Maven JVM");
        return value;
    }

    private String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing required property: " + name);
        return value;
    }

    private String safeForLog(String content) {
        return content.replace('\n', ' ').replace('\r', ' ');
    }
}
