package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "ai.vector.it.enabled", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgVectorStoreIntegrationTest {

    private JdbcTemplate jdbcTemplate;
    private PgVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        String url = requiredProperty("ai.vector.it.jdbc-url");
        String username = requiredProperty("ai.vector.it.username");
        String password = requiredProperty("ai.vector.it.password");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        jdbcTemplate = new JdbcTemplate(dataSource);
        vectorStore = PgVectorStore.builder(jdbcTemplate, new DeterministicEmbeddingModel())
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .build();
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM vector_ingestion_manifest");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM vector_ingestion_manifest");
    }

    @Test
    void stores1536DimensionEmbeddingsAndSearchesTopKWithThresholdAndFilters() {
        Document hueRoute = document("route-hue", "Tuyến xe từ Hà Nội đến Huế", Map.of(
                "sourceType", "route", "sourceId", "1", "routeId", "1"));
        Document hueTrip = document("trip-hue", "Chuyến xe khởi hành đi Huế", Map.of(
                "sourceType", "trip", "sourceId", "2", "routeId", "1"));
        Document danangRoute = document("route-danang", "Tuyến xe từ Hà Nội đến Đà Nẵng", Map.of(
                "sourceType", "route", "sourceId", "3", "routeId", "3"));
        vectorStore.add(List.of(hueRoute, hueTrip, danangRoute));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT vector_dims(embedding) FROM vector_store LIMIT 1", Integer.class))
                .isEqualTo(1536);

        List<Document> topK = vectorStore.similaritySearch(SearchRequest.builder()
                .query("xe đi Huế")
                .topK(2)
                .similarityThreshold(0.7d)
                .build());
        assertThat(topK).hasSize(2);
        assertThat(topK).allSatisfy(document -> assertThat(document.getText()).containsIgnoringCase("Huế"));

        PgVectorSearchService searchService = new PgVectorSearchService(vectorStore, new AllowAllVectorCollectionGuard());
        assertThat(searchService.search(new VectorSearchRequest("xe đi Huế", 5, 0.7d, "ROUTE", null)))
                .extracting(VectorSearchResult::documentId).containsExactly(hueRoute.getId());
        assertThat(searchService.search(new VectorSearchRequest("xe đi Huế", 5, 0.7d, null, 1L)))
                .extracting(VectorSearchResult::documentId).containsExactly(hueRoute.getId(), hueTrip.getId());
        assertThat(searchService.search(new VectorSearchRequest("xe đi Huế", 5, 0.7d, "TRIP", 1L)))
                .extracting(VectorSearchResult::documentId).containsExactly(hueTrip.getId());
    }

    @Test
    void ingestionIsIdempotentUpdatesChangedSourcesRemovesStaleChunksAndDeletesRemovedSources() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        TripRepository tripRepository = mock(TripRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        when(tripRepository.findAll()).thenReturn(List.of());
        when(vehicleRepository.findAll()).thenReturn(List.of());
        Route route = Route.builder().id(10L).departureLocation("Hà Nội").arrivalLocation("Huế").build();
        when(routeRepository.findAll()).thenReturn(List.of(route));
        VectorIngestionService service = ingestionService(routeRepository, tripRepository, vehicleRepository,
                new JdbcVectorIngestionStateStore(jdbcTemplate));

        service.ingestAll();
        service.ingestAll();
        assertCounts(1, 1);

        route.setArrivalLocation("Đà Nẵng");
        service.ingestAll();
        assertCounts(1, 1);
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM vector_store", String.class)).contains("Đà Nẵng");

        route.setDepartureLocation("Hà Nội ".repeat(2_500));
        service.ingestAll();
        int expandedCount = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
        assertThat(expandedCount).isGreaterThan(1);
        route.setDepartureLocation("Hà Nội");
        service.ingestAll();
        assertCounts(1, 1);

        when(routeRepository.findAll()).thenReturn(List.of());
        service.ingestAll();
        assertCounts(0, 0);
    }

    @Test
    void retryAfterManifestFailureUsesUpsertWithoutDuplicateVectors() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        TripRepository tripRepository = mock(TripRepository.class);
        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        when(routeRepository.findAll()).thenReturn(List.of(Route.builder().id(20L).departureLocation("Huế").arrivalLocation("Đà Nẵng").build()));
        when(tripRepository.findAll()).thenReturn(List.of());
        when(vehicleRepository.findAll()).thenReturn(List.of());

        VectorIngestionStateStore failingStateStore = new VectorIngestionStateStore() {
            @Override public java.util.Optional<VectorIngestionState> find(String sourceType, String sourceId) { return java.util.Optional.empty(); }
            @Override public List<VectorIngestionState> findAll() { return List.of(); }
            @Override public void save(VectorIngestionState state) { throw new IllegalStateException("manifest unavailable"); }
            @Override public void delete(VectorIngestionState state) { }
        };
        ingestionService(routeRepository, tripRepository, vehicleRepository, failingStateStore).ingestAll();
        assertCounts(1, 0);

        ingestionService(routeRepository, tripRepository, vehicleRepository, new JdbcVectorIngestionStateStore(jdbcTemplate)).ingestAll();
        assertCounts(1, 1);
    }

    private VectorIngestionService ingestionService(RouteRepository routeRepository, TripRepository tripRepository,
                                                    VehicleRepository vehicleRepository, VectorIngestionStateStore stateStore) {
        return new VectorIngestionService(routeRepository, tripRepository, vehicleRepository,
                new VectorDocumentMapper(), vectorStore, stateStore, new AllowAllVectorCollectionGuard());
    }

    private void assertCounts(int vectors, int manifests) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(vectors);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_ingestion_manifest", Integer.class)).isEqualTo(manifests);
    }

    private Document document(String name, String text, Map<String, Object> metadata) {
        return Document.builder().id(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString())
                .text(text).metadata(metadata).build();
    }

    private String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing integration-test property: " + name);
        }
        return value;
    }

    private static final class DeterministicEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new java.util.ArrayList<>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                embeddings.add(new Embedding(vector(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(embeddings);
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
            float[] vector = new float[1536];
            String normalized = text.toLowerCase(Locale.ROOT);
            if (normalized.contains("huế")) {
                vector[0] = 1f;
            }
            else if (normalized.contains("đà nẵng")) {
                vector[1] = 1f;
            }
            else {
                vector[2] = 1f;
            }
            return vector;
        }
    }
}
