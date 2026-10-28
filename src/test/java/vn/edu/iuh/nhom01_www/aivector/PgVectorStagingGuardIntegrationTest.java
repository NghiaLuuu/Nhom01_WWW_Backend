package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies an existing unregistered staging collection is blocked without changing it. */
@EnabledIfSystemProperty(named = "ai.vector.guard.it.enabled", matches = "true")
class PgVectorStagingGuardIntegrationTest {

    @Test
    void blocksExistingUnregisteredVectorsWithoutDeletingThem() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                required("ai.vector.it.jdbc-url"), required("ai.vector.it.username"), required("ai.vector.it.password")));
        int vectorsBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
        int metadataBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_collection_metadata", Integer.class);
        assertThat(vectorsBefore).isEqualTo(80);
        assertThat(metadataBefore).isZero();

        VectorEmbeddingProperties properties = new VectorEmbeddingProperties();
        properties.setCollectionName("vector_store");
        properties.setProvider("openai");
        properties.setModel("text-embedding-3-small");
        properties.setDimensions(1536);
        properties.setConfigurationVersion("v1");
        EmbeddingModel dimensionsOnlyModel = new EmbeddingModel() {
            @Override public EmbeddingResponse call(EmbeddingRequest request) { throw new UnsupportedOperationException(); }
            @Override public float[] embed(Document document) { throw new UnsupportedOperationException(); }
            @Override public int dimensions() { return 1536; }
        };
        VectorCollectionGuard guard = new JdbcVectorCollectionGuard(
                new JdbcVectorCollectionMetadataStore(jdbcTemplate), properties, dimensionsOnlyModel);

        assertThatThrownBy(guard::verifyReadAllowed).isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("not registered");
        assertThatThrownBy(guard::verifyWriteAllowed).isInstanceOf(VectorCollectionModelMismatchException.class)
                .hasMessageContaining("unregistered vectors");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(vectorsBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_collection_metadata", Integer.class)).isEqualTo(metadataBefore);
    }

    private String required(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing staging property: " + name);
        }
        return value;
    }
}
