package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcVectorCollectionGuardTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private InMemoryMetadataStore metadataStore;
    private JdbcVectorCollectionGuard guard;
    private VectorEmbeddingProperties properties;

    @BeforeEach
    void setUp() {
        metadataStore = new InMemoryMetadataStore();
        properties = properties("openai", "text-embedding-3-small", 1536, "v1");
        when(embeddingModel.dimensions()).thenReturn(1536);
        guard = new JdbcVectorCollectionGuard(metadataStore, properties, embeddingModel);
    }

    @Test
    void registersIdentityOnlyForAnEmptyCollection() {
        guard.verifyWriteAllowed();

        assertThat(metadataStore.find("vector_store")).contains(new VectorCollectionMetadata("vector_store",
                new EmbeddingIdentity("openai", "text-embedding-3-small", 1536, "v1")));
        guard.verifyReadAllowed();
    }

    @Test
    void blocksUnregisteredCollectionThatAlreadyContainsVectors() {
        metadataStore.vectorCount = 80;

        assertThatThrownBy(guard::verifyWriteAllowed)
                .isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("unregistered vectors");
    }

    @Test
    void blocksConfiguredModelMismatchForReadAndWrite() {
        metadataStore.insert(new VectorCollectionMetadata("vector_store",
                new EmbeddingIdentity("test", "vietnamese-lexical-embedding", 1536, "staging-v1")));

        assertThatThrownBy(guard::verifyReadAllowed)
                .isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("mismatch");
        assertThatThrownBy(guard::verifyWriteAllowed)
                .isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    void blocksWhenActiveEmbeddingModelDimensionsDoNotMatchConfiguredIdentity() {
        when(embeddingModel.dimensions()).thenReturn(1024);

        assertThatThrownBy(guard::verifyWriteAllowed)
                .isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("dimensions");
    }

    private VectorEmbeddingProperties properties(String provider, String model, int dimensions, String version) {
        VectorEmbeddingProperties result = new VectorEmbeddingProperties();
        result.setCollectionName("vector_store");
        result.setProvider(provider);
        result.setModel(model);
        result.setDimensions(dimensions);
        result.setConfigurationVersion(version);
        return result;
    }

    private static final class InMemoryMetadataStore implements VectorCollectionMetadataStore {
        private final Map<String, VectorCollectionMetadata> entries = new HashMap<>();
        private long vectorCount;

        @Override
        public Optional<VectorCollectionMetadata> find(String collectionName) {
            return Optional.ofNullable(entries.get(collectionName));
        }

        @Override
        public long vectorCount(String collectionName) {
            return vectorCount;
        }

        @Override
        public void insert(VectorCollectionMetadata metadata) {
            entries.put(metadata.collectionName(), metadata);
        }
    }
}
