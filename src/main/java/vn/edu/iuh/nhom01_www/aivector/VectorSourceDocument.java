package vn.edu.iuh.nhom01_www.aivector;

import org.springframework.ai.document.Document;

import java.util.List;

record VectorSourceDocument(String sourceType, String sourceId, String documentVersion, List<Document> documents) {

    String sourceKey() {
        return sourceType + ":" + sourceId;
    }
}
