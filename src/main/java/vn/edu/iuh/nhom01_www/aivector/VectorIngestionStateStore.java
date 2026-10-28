package vn.edu.iuh.nhom01_www.aivector;

import java.util.List;
import java.util.Optional;

interface VectorIngestionStateStore {

    Optional<VectorIngestionState> find(String sourceType, String sourceId);

    List<VectorIngestionState> findAll();

    void save(VectorIngestionState state);

    void delete(VectorIngestionState state);
}
