package vn.edu.iuh.nhom01_www.aivector;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("ai-vector")
@RequiredArgsConstructor
class JdbcVectorIngestionStateStore implements VectorIngestionStateStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<VectorIngestionState> find(String sourceType, String sourceId) {
        List<VectorIngestionState> states = jdbcTemplate.query("""
                SELECT source_type, source_id, document_version, chunk_count
                FROM vector_ingestion_manifest
                WHERE source_type = ? AND source_id = ?
                """, (resultSet, rowNum) -> map(resultSet), sourceType, sourceId);
        return states.stream().findFirst();
    }

    @Override
    public List<VectorIngestionState> findAll() {
        return jdbcTemplate.query("""
                SELECT source_type, source_id, document_version, chunk_count
                FROM vector_ingestion_manifest
                """, (resultSet, rowNum) -> map(resultSet));
    }

    @Override
    public void save(VectorIngestionState state) {
        jdbcTemplate.update("""
                INSERT INTO vector_ingestion_manifest (source_type, source_id, document_version, chunk_count)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (source_type, source_id) DO UPDATE
                SET document_version = EXCLUDED.document_version,
                    chunk_count = EXCLUDED.chunk_count,
                    updated_at = CURRENT_TIMESTAMP
                """, state.sourceType(), state.sourceId(), state.documentVersion(), state.chunkCount());
    }

    @Override
    public void delete(VectorIngestionState state) {
        jdbcTemplate.update("DELETE FROM vector_ingestion_manifest WHERE source_type = ? AND source_id = ?",
                state.sourceType(), state.sourceId());
    }

    private VectorIngestionState map(ResultSet resultSet) throws java.sql.SQLException {
        return new VectorIngestionState(
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getString("document_version"),
                resultSet.getInt("chunk_count"));
    }
}
