package vn.edu.iuh.nhom01_www.aivector;

import java.util.Map;

public record VectorSearchResult(String documentId, String content, Map<String, Object> metadata, Double similarityScore) {
}
