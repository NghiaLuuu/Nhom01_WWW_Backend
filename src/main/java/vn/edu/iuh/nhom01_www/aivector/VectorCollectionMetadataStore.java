package vn.edu.iuh.nhom01_www.aivector;

import java.util.Optional;

interface VectorCollectionMetadataStore {

    Optional<VectorCollectionMetadata> find(String collectionName);

    long vectorCount(String collectionName);

    void insert(VectorCollectionMetadata metadata);
}
