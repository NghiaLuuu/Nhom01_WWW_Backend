package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgVectorSearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    private PgVectorSearchService service;

    @BeforeEach
    void setUp() {
        service = new PgVectorSearchService(vectorStore, new AllowAllVectorCollectionGuard());
    }

    @Test
    void searchesVietnameseQueryAndPreservesVectorStoreRanking() {
        Document first = Document.builder().id("first").text("Tuyến Hà Nội đến Huế")
                .metadata(Map.of("sourceType", "route")).score(0.91d).build();
        Document second = Document.builder().id("second").text("Chuyến Hà Nội đến Huế")
                .metadata(Map.of("sourceType", "trip")).score(0.82d).build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(first, second));

        List<VectorSearchResult> results = service.search(new VectorSearchRequest("Xe đi Huế", 2, 0.75d, null, null));

        assertThat(results).extracting(VectorSearchResult::documentId).containsExactly("first", "second");
        assertThat(results.get(0).similarityScore()).isEqualTo(0.91d);
        assertThat(results.get(0).metadata()).containsEntry("sourceType", "route");
    }

    @Test
    void usesDefaultsWhenTopKAndThresholdAreOmitted() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Tìm tuyến xe", null, null, null, null));

        SearchRequest request = capturedRequest();
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.35d);
        assertThat(request.hasFilterExpression()).isFalse();
    }

    @Test
    void acceptsMaximumTopKAndRejectsInvalidLimits() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Tìm tuyến xe", 20, 0d, null, null));
        assertThat(capturedRequest().getTopK()).isEqualTo(20);
        assertThatThrownBy(() -> service.search(new VectorSearchRequest("Tìm tuyến xe", 21, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("topK");
        assertThatThrownBy(() -> service.search(new VectorSearchRequest("Tìm tuyến xe", 0, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("topK");
    }

    @Test
    void appliesSourceTypeFilterWithFixedMetadataKey() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Xe giường nằm", null, null, "VEHICLE", null));

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("sourceType", "vehicle");
    }

    @Test
    void appliesRouteIdFilterWithFixedMetadataKey() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("chuyến đi Huế", null, null, null, 12L));

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("routeId", "12");
    }

    @Test
    void combinesSourceTypeAndRouteIdFiltersWithoutTextConcatenation() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("chuyến đi Huế", null, null, "TRIP", 12L));

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("AND", "sourceType", "trip", "routeId", "12");
    }

    @Test
    void usesCanonicalDirectionalMetadataToPreventReverseRouteRanking() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Tôi muốn đi từ Sài Gòn đến Đà Lạt", 5, 0d, null, null));

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("departureLocationNormalized", "tp hcm")
                .contains("arrivalLocationNormalized", "da lat");
    }

    @Test
    void preservesTheRequestedReverseDirectionAndCanonicalizesHoChiMinh() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Từ Đà Lạt đến Hồ Chí Minh", 5, 0d, null, null));

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("departureLocationNormalized", "da lat")
                .contains("arrivalLocationNormalized", "tp hcm");
    }

    @Test
    void preservesSemanticOnlySearchWhenNoDirectionCanBeExtracted() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Tìm xe giường nằm", 5, 0d, null, null));

        assertThat(capturedRequest().hasFilterExpression()).isFalse();
    }

    @Test
    void returnsNoResultForAnExactDirectionThatDoesNotExistInsteadOfAReverseRoute() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertThat(service.search(new VectorSearchRequest("Từ Cà Mau đến Đà Lạt", 5, 0d, null, null))).isEmpty();

        String filter = capturedRequest().getFilterExpression().toString();
        assertThat(filter).contains("departureLocationNormalized", "ca mau")
                .contains("arrivalLocationNormalized", "da lat");
    }

    @Test
    void leavesMultiLegQueriesToSemanticRetrievalInsteadOfApplyingAnIncorrectDirectionalFilter() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search(new VectorSearchRequest("Từ TP.HCM đến Đà Lạt rồi đến Nha Trang", 5, 0d, null, null));

        assertThat(capturedRequest().hasFilterExpression()).isFalse();
    }

    @Test
    void returnsNullableScoreAndEmptyListWithoutSyntheticResults() {
        Document noScore = new Document("document-id", "Nội dung", Map.of("sourceType", "route"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(noScore));

        List<VectorSearchResult> results = service.search(new VectorSearchRequest("nội dung", null, null, null, null));

        assertThat(results).singleElement().satisfies(result -> assertThat(result.similarityScore()).isNull());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        assertThat(service.search(new VectorSearchRequest("không có", null, null, null, null))).isEmpty();
    }

    @Test
    void rejectsBlankInvalidThresholdAndInvalidSourceType() {
        assertThatThrownBy(() -> service.search(new VectorSearchRequest(" ", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query");
        assertThatThrownBy(() -> service.search(new VectorSearchRequest("x", null, 1.1d, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("similarityThreshold");
        assertThatThrownBy(() -> service.search(new VectorSearchRequest("x", null, null, "SQL", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sourceType");
    }

    @Test
    void wrapsVectorStoreFailures() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.search(new VectorSearchRequest("Tìm xe", null, null, null, null)))
                .isInstanceOf(VectorSearchException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void searchBeanRequiresAiVectorProfile() {
        assertThat(PgVectorSearchService.class.getAnnotation(Profile.class).value()).containsExactly("ai-vector");
    }

    private SearchRequest capturedRequest() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        return captor.getValue();
    }
}
