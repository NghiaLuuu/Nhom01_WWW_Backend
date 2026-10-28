package vn.edu.iuh.nhom01_www.aivector;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("ai-vector")
@RequiredArgsConstructor
class JdbcVectorCollectionMetadataStore implements VectorCollectionMetadataStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<VectorCollectionMetadata> find(String collectionName) {
        List<VectorCollectionMetadata> metadata = jdbcTemplate.query("""
                SELECT collection_name, embedding_provider, embedding_model, embedding_dimensions, configuration_version
                FROM vector_collection_metadata
                WHERE collection_name = ?
                """, (resultSet, rowNumber) -> new VectorCollectionMetadata(
                resultSet.getString("collection_name"),
                new EmbeddingIdentity(resultSet.getString("embedding_provider"), resultSet.getString("embedding_model"),
                        resultSet.getInt("embedding_dimensions"), resultSet.getString("configuration_version"))), collectionName);
        return metadata.stream().findFirst();
    }

    @Override
    public long vectorCount(String collectionName) {
        if (!collectionName.matches("[A-Za-z_][A-Za-z0-9_]{0,62}")) {
            throw new VectorCollectionModelMismatchException("Invalid vector collection name");
        }
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + collectionName, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public void insert(VectorCollectionMetadata metadata) {
        jdbcTemplate.update("""
                INSERT INTO vector_collection_metadata (
                    collection_name, embedding_provider, embedding_model, embedding_dimensions, configuration_version)
                VALUES (?, ?, ?, ?, ?)
                """, metadata.collectionName(), metadata.embeddingIdentity().provider(), metadata.embeddingIdentity().model(),
                metadata.embeddingIdentity().dimensions(), metadata.embeddingIdentity().configurationVersion());
    }
}
