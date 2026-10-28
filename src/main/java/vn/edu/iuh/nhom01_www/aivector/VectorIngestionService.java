package vn.edu.iuh.nhom01_www.aivector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Profile("ai-vector")
@RequiredArgsConstructor
public class VectorIngestionService {

    private static final Set<String> MANAGED_SOURCE_TYPES = Set.of("route", "trip", "vehicle");

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final VectorDocumentMapper documentMapper;
    private final VectorStore vectorStore;
    private final VectorIngestionStateStore stateStore;
    private final VectorCollectionGuard collectionGuard;

    @Value("${app.ai-vector.ingestion.batch-size:100}")
    private int batchSize = 100;

    /**
     * Explicit entry point for an administrator job or a later chatbot integration.
     * This service intentionally has no startup runner or scheduled task.
     */
    @Transactional
    public VectorIngestionReport ingestAll() {
        collectionGuard.verifyWriteAllowed();
        List<VectorSourceDocument> mappedSources = new ArrayList<>();
        Set<String> currentSourceKeys = new HashSet<>();
        Counters counters = new Counters();

        routeRepository.findAll().forEach(route -> {
            counters.sourcesRead++;
            observe(route == null ? null : route.getId(), "route", currentSourceKeys);
            addMapped(documentMapper.map(route), mappedSources, counters);
        });
        tripRepository.findAll().forEach(trip -> {
            counters.sourcesRead++;
            observe(trip == null ? null : trip.getId(), "trip", currentSourceKeys);
            addMapped(documentMapper.map(trip), mappedSources, counters);
        });
        vehicleRepository.findAll().forEach(vehicle -> {
            counters.sourcesRead++;
            observe(vehicle == null ? null : vehicle.getId(), "vehicle", currentSourceKeys);
            addMapped(documentMapper.map(vehicle), mappedSources, counters);
        });

        List<PreparedSource> pendingWrites = mappedSources.stream()
                .map(this::prepare)
                .flatMap(Optional::stream)
                .toList();
        writeInBatches(pendingWrites, counters);
        removeMissingSources(currentSourceKeys, counters);

        VectorIngestionReport report = new VectorIngestionReport(counters.sourcesRead, counters.sourcesSkipped,
                counters.sourcesSucceeded, counters.sourcesFailed, counters.documentsWritten, counters.sourcesDeleted);
        log.info("AI Vector ingestion completed: read={}, skipped={}, succeeded={}, failed={}, documents={}, deleted={}",
                report.sourcesRead(), report.sourcesSkipped(), report.sourcesSucceeded(), report.sourcesFailed(),
                report.documentsWritten(), report.sourcesDeleted());
        return report;
    }

    private void addMapped(Optional<VectorSourceDocument> mappedSource, List<VectorSourceDocument> mappedSources,
                           Counters counters) {
        if (mappedSource.isPresent()) {
            mappedSources.add(mappedSource.get());
        }
        else {
            counters.sourcesSkipped++;
        }
    }

    private void observe(Long id, String sourceType, Set<String> currentSourceKeys) {
        if (id != null) {
            currentSourceKeys.add(sourceType + ":" + id);
        }
    }

    private Optional<PreparedSource> prepare(VectorSourceDocument source) {
        Optional<VectorIngestionState> currentState = stateStore.find(source.sourceType(), source.sourceId());
        if (currentState.isPresent() && currentState.get().documentVersion().equals(source.documentVersion())
                && currentState.get().chunkCount() == source.documents().size()) {
            return Optional.empty();
        }
        return Optional.of(new PreparedSource(source, currentState.orElse(null)));
    }

    private void writeInBatches(List<PreparedSource> sources, Counters counters) {
        List<PreparedSource> batch = new ArrayList<>();
        int documentsInBatch = 0;
        for (PreparedSource source : sources) {
            int sourceDocumentCount = source.source().documents().size();
            if (!batch.isEmpty() && documentsInBatch + sourceDocumentCount > effectiveBatchSize()) {
                writeBatch(batch, counters);
                batch = new ArrayList<>();
                documentsInBatch = 0;
            }
            batch.add(source);
            documentsInBatch += sourceDocumentCount;
            if (documentsInBatch >= effectiveBatchSize()) {
                writeBatch(batch, counters);
                batch = new ArrayList<>();
                documentsInBatch = 0;
            }
        }
        if (!batch.isEmpty()) {
            writeBatch(batch, counters);
        }
    }

    private void writeBatch(List<PreparedSource> batch, Counters counters) {
        List<Document> documents = batch.stream().flatMap(source -> source.source().documents().stream()).toList();
        try {
            vectorStore.add(documents);
        }
        catch (RuntimeException exception) {
            counters.sourcesFailed += batch.size();
            log.warn("AI Vector ingestion failed for {} source(s); existing manifest and stale vectors were retained", batch.size(), exception);
            return;
        }

        for (PreparedSource source : batch) {
            try {
                removeStaleChunks(source);
                stateStore.save(new VectorIngestionState(source.source().sourceType(), source.source().sourceId(),
                        source.source().documentVersion(), source.source().documents().size()));
                counters.sourcesSucceeded++;
                counters.documentsWritten += source.source().documents().size();
            }
            catch (RuntimeException exception) {
                counters.sourcesFailed++;
                log.warn("AI Vector ingestion wrote new vectors but retained the previous manifest for {} so cleanup can retry safely",
                        source.source().sourceKey(), exception);
            }
        }
    }

    private void removeStaleChunks(PreparedSource source) {
        if (source.previousState() == null || source.previousState().chunkCount() <= source.source().documents().size()) {
            return;
        }
        List<String> staleIds = new ArrayList<>();
        for (int index = source.source().documents().size(); index < source.previousState().chunkCount(); index++) {
            staleIds.add(VectorDocumentMapper.stableDocumentId(source.source().sourceType(), source.source().sourceId(), index));
        }
        vectorStore.delete(staleIds);
    }

    private void removeMissingSources(Set<String> currentSourceKeys, Counters counters) {
        for (VectorIngestionState state : stateStore.findAll()) {
            if (!MANAGED_SOURCE_TYPES.contains(state.sourceType()) || currentSourceKeys.contains(state.sourceKey())) {
                continue;
            }
            try {
                List<String> documentIds = new ArrayList<>();
                for (int index = 0; index < state.chunkCount(); index++) {
                    documentIds.add(VectorDocumentMapper.stableDocumentId(state.sourceType(), state.sourceId(), index));
                }
                vectorStore.delete(documentIds);
                stateStore.delete(state);
                counters.sourcesDeleted++;
            }
            catch (RuntimeException exception) {
                counters.sourcesFailed++;
                log.warn("AI Vector deletion failed for removed source {}; manifest was retained for retry", state.sourceKey(), exception);
            }
        }
    }

    private int effectiveBatchSize() {
        return Math.max(1, batchSize);
    }

    private record PreparedSource(VectorSourceDocument source, VectorIngestionState previousState) {
    }

    private static class Counters {
        private int sourcesRead;
        private int sourcesSkipped;
        private int sourcesSucceeded;
        private int sourcesFailed;
        private int documentsWritten;
        private int sourcesDeleted;
    }
}
