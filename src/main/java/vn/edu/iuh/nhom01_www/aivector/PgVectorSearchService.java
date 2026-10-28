package vn.edu.iuh.nhom01_www.aivector;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("ai-vector")
@Validated
@RequiredArgsConstructor
public class PgVectorSearchService implements VectorSearchService {

    static final int DEFAULT_TOP_K = 5;
    static final int MAX_TOP_K = 20;
    // OpenAI cosine scores for the Vietnamese corpus are empirically around 0.45-0.55.
    // Directional metadata remains the correctness constraint; this threshold is recall-only.
    static final double DEFAULT_SIMILARITY_THRESHOLD = 0.35d;

    private final VectorStore vectorStore;
    private final VectorCollectionGuard collectionGuard;
    private final VietnameseLocationNormalizer locationNormalizer = new VietnameseLocationNormalizer();

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        collectionGuard.verifyReadAllowed();
        ValidatedSearchRequest validated = validate(request);
        SearchRequest.Builder search = SearchRequest.builder()
                .query(validated.query())
                .topK(validated.topK())
                .similarityThreshold(validated.similarityThreshold());
        Filter.Expression filter = metadataFilter(validated.sourceType(), validated.routeId(), validated.directionalLocation());
        if (filter != null) {
            search.filterExpression(filter);
        }

        try {
            return vectorStore.similaritySearch(search.build()).stream()
                    .map(this::toResult)
                    .toList();
        }
        catch (RuntimeException exception) {
            throw new VectorSearchException("Vector search failed", exception);
        }
    }

    private ValidatedSearchRequest validate(VectorSearchRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }
        double threshold = request.similarityThreshold() == null
                ? DEFAULT_SIMILARITY_THRESHOLD : request.similarityThreshold();
        if (!Double.isFinite(threshold) || threshold < 0d || threshold > 1d) {
            throw new IllegalArgumentException("similarityThreshold must be between 0.0 and 1.0");
        }
        String sourceType = request.sourceType() == null ? null : normalizeSourceType(request.sourceType());
        if (request.routeId() != null && request.routeId() <= 0) {
            throw new IllegalArgumentException("routeId must be positive");
        }
        String query = request.query().trim();
        return new ValidatedSearchRequest(query, topK, threshold, sourceType, request.routeId(),
                locationNormalizer.directionalLocations(query).orElse(null));
    }

    private String normalizeSourceType(String sourceType) {
        return switch (sourceType.trim().toUpperCase(Locale.ROOT)) {
            case "ROUTE" -> "route";
            case "TRIP" -> "trip";
            case "VEHICLE" -> "vehicle";
            default -> throw new IllegalArgumentException("sourceType must be ROUTE, TRIP, or VEHICLE");
        };
    }

    private Filter.Expression metadataFilter(String sourceType, Long routeId,
                                             VietnameseLocationNormalizer.DirectionalLocation directionalLocation) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op combined = sourceType == null ? null : filter.eq("sourceType", sourceType);
        if (routeId != null) {
            combined = and(filter, combined, filter.eq("routeId", routeId.toString()));
        }
        if (directionalLocation != null) {
            combined = and(filter, combined,
                    filter.eq("departureLocationNormalized", directionalLocation.departure()));
            combined = and(filter, combined,
                    filter.eq("arrivalLocationNormalized", directionalLocation.arrival()));
        }
        return combined == null ? null : combined.build();
    }

    private FilterExpressionBuilder.Op and(FilterExpressionBuilder filter, FilterExpressionBuilder.Op left,
                                           FilterExpressionBuilder.Op right) {
        return left == null ? right : filter.and(left, right);
    }

    private VectorSearchResult toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata() == null ? Map.of() : Map.copyOf(document.getMetadata());
        return new VectorSearchResult(document.getId(), document.getText(), metadata, document.getScore());
    }

    private record ValidatedSearchRequest(String query, int topK, double similarityThreshold, String sourceType,
                                          Long routeId,
                                          VietnameseLocationNormalizer.DirectionalLocation directionalLocation) {
    }
}
