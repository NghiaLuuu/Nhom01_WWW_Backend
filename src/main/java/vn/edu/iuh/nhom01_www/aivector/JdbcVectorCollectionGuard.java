package vn.edu.iuh.nhom01_www.aivector;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("ai-vector")
@RequiredArgsConstructor
class JdbcVectorCollectionGuard implements VectorCollectionGuard {

    private final VectorCollectionMetadataStore metadataStore;
    private final VectorEmbeddingProperties embeddingProperties;
    private final EmbeddingModel embeddingModel;

    @Override
    public void verifyReadAllowed() {
        EmbeddingIdentity expected = expectedIdentity();
        VectorCollectionMetadata metadata = metadataStore.find(embeddingProperties.getCollectionName())
                .orElseThrow(() -> new VectorCollectionModelMismatchException(
                        "Vector collection is not registered; run a controlled re-index before searching"));
        assertMatches(metadata.embeddingIdentity(), expected);
    }

    @Override
    public void verifyWriteAllowed() {
        EmbeddingIdentity expected = expectedIdentity();
        Optional<VectorCollectionMetadata> existing = metadataStore.find(embeddingProperties.getCollectionName());
        if (existing.isPresent()) {
            assertMatches(existing.get().embeddingIdentity(), expected);
            return;
        }
        if (metadataStore.vectorCount(embeddingProperties.getCollectionName()) > 0) {
            throw new VectorCollectionModelMismatchException(
                    "Vector collection contains unregistered vectors; controlled re-index or explicit registration is required");
        }
        try {
            metadataStore.insert(new VectorCollectionMetadata(embeddingProperties.getCollectionName(), expected));
        }
        catch (DuplicateKeyException exception) {
            VectorCollectionMetadata insertedByAnotherWriter = metadataStore.find(embeddingProperties.getCollectionName())
                    .orElseThrow(() -> exception);
            assertMatches(insertedByAnotherWriter.embeddingIdentity(), expected);
        }
    }

    private EmbeddingIdentity expectedIdentity() {
        EmbeddingIdentity identity = embeddingProperties.identity();
        if (isBlank(identity.provider()) || isBlank(identity.model()) || isBlank(identity.configurationVersion())
                || identity.dimensions() <= 0) {
            throw new VectorCollectionModelMismatchException("Embedding identity configuration is incomplete");
        }
        int modelDimensions = embeddingModel.dimensions();
        if (modelDimensions > 0 && modelDimensions != identity.dimensions()) {
            throw new VectorCollectionModelMismatchException("Configured embedding dimensions do not match the active EmbeddingModel");
        }
        return identity;
    }

    private void assertMatches(EmbeddingIdentity actual, EmbeddingIdentity expected) {
        if (!actual.equals(expected)) {
            throw new VectorCollectionModelMismatchException("Embedding identity mismatch for vector collection; controlled re-index is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
