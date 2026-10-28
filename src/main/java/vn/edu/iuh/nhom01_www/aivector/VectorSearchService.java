package vn.edu.iuh.nhom01_www.aivector;

import java.util.List;

/**
 * Read-only semantic search contract for consumers such as the chatbot module.
 */
public interface VectorSearchService {

    List<VectorSearchResult> search(VectorSearchRequest request);
}
