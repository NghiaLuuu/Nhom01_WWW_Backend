package vn.edu.iuh.nhom01_www.aivector;

record VectorIngestionState(String sourceType, String sourceId, String documentVersion, int chunkCount) {

    String sourceKey() {
        return sourceType + ":" + sourceId;
    }
}
