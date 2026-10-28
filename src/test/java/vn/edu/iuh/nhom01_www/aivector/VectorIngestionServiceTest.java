package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorIngestionServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VectorStore vectorStore;

    private InMemoryStateStore stateStore;
    private VectorIngestionService service;

    @BeforeEach
    void setUp() {
        stateStore = new InMemoryStateStore();
        service = new VectorIngestionService(routeRepository, tripRepository, vehicleRepository,
                new VectorDocumentMapper(), vectorStore, stateStore, new AllowAllVectorCollectionGuard());
        ReflectionTestUtils.setField(service, "batchSize", 2);
        when(tripRepository.findAll()).thenReturn(List.of());
        when(vehicleRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void writesValidSourcesInBatchesAndSkipsInvalidOnes() {
        when(routeRepository.findAll()).thenReturn(List.of(route(1L, "Hà Nội", "Huế"),
                route(2L, "Đà Nẵng", "Quy Nhơn"), Route.builder().id(3L).arrivalLocation("Huế").build()));

        VectorIngestionReport report = service.ingestAll();

        ArgumentCaptor<List<Document>> documents = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documents.capture());
        assertThat(documents.getValue()).hasSize(2);
        assertThat(report).isEqualTo(new VectorIngestionReport(3, 1, 2, 0, 2, 0));
    }

    @Test
    void secondRunWithSameContentDoesNotWriteDuplicateDocuments() {
        when(routeRepository.findAll()).thenReturn(List.of(route(1L, "Hà Nội", "Huế")));

        service.ingestAll();
        service.ingestAll();

        verify(vectorStore).add(anyList());
        assertThat(stateStore.find("route", "1")).isPresent();
    }

    @Test
    void changedSourceUpsertsSameStableDocumentId() {
        Route route = route(1L, "Hà Nội", "Huế");
        when(routeRepository.findAll()).thenReturn(List.of(route));
        service.ingestAll();
        String firstVersion = stateStore.find("route", "1").orElseThrow().documentVersion();

        route.setArrivalLocation("Đà Nẵng");
        service.ingestAll();

        ArgumentCaptor<List<Document>> documents = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, org.mockito.Mockito.times(2)).add(documents.capture());
        assertThat(documents.getAllValues().get(0).get(0).getId())
                .isEqualTo(documents.getAllValues().get(1).get(0).getId());
        assertThat(stateStore.find("route", "1").orElseThrow().documentVersion()).isNotEqualTo(firstVersion);
    }

    @Test
    void reducedChunkCountDeletesOnlyStaleStableIdsAfterSuccessfulWrite() {
        stateStore.save(new VectorIngestionState("route", "1", "old-version", 2));
        when(routeRepository.findAll()).thenReturn(List.of(route(1L, "Hà Nội", "Huế")));

        service.ingestAll();

        ArgumentCaptor<List<String>> ids = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).delete(ids.capture());
        assertThat(ids.getValue()).containsExactly(VectorDocumentMapper.stableDocumentId("route", "1", 1));
        assertThat(stateStore.find("route", "1").orElseThrow().chunkCount()).isEqualTo(1);
    }

    @Test
    void removedSourceDeletesItsDocumentsAndManifest() {
        when(routeRepository.findAll()).thenReturn(List.of(route(1L, "Hà Nội", "Huế")));
        service.ingestAll();
        when(routeRepository.findAll()).thenReturn(List.of());

        VectorIngestionReport report = service.ingestAll();

        verify(vectorStore).delete(List.of(VectorDocumentMapper.stableDocumentId("route", "1", 0)));
        assertThat(stateStore.find("route", "1")).isEmpty();
        assertThat(report.sourcesDeleted()).isEqualTo(1);
    }

    @Test
    void failedVectorWriteKeepsManifestUnsetForSafeRetry() {
        when(routeRepository.findAll()).thenReturn(List.of(route(1L, "Hà Nội", "Huế")));
        doThrow(new IllegalStateException("vector store unavailable")).when(vectorStore).add(anyList());

        VectorIngestionReport report = service.ingestAll();

        assertThat(report.sourcesFailed()).isEqualTo(1);
        assertThat(stateStore.find("route", "1")).isEmpty();
        verify(vectorStore, never()).delete(org.mockito.ArgumentMatchers.anyList());
    }

    private Route route(Long id, String departure, String arrival) {
        return Route.builder().id(id).departureLocation(departure).arrivalLocation(arrival).build();
    }

    private static final class InMemoryStateStore implements VectorIngestionStateStore {
        private final Map<String, VectorIngestionState> states = new HashMap<>();

        @Override
        public Optional<VectorIngestionState> find(String sourceType, String sourceId) {
            return Optional.ofNullable(states.get(sourceType + ":" + sourceId));
        }

        @Override
        public List<VectorIngestionState> findAll() {
            return new ArrayList<>(states.values());
        }

        @Override
        public void save(VectorIngestionState state) {
            states.put(state.sourceKey(), state);
        }

        @Override
        public void delete(VectorIngestionState state) {
            states.remove(state.sourceKey());
        }
    }
}
