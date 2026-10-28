package vn.edu.iuh.nhom01_www.aivector;

public record VectorIngestionReport(int sourcesRead, int sourcesSkipped, int sourcesSucceeded, int sourcesFailed,
                                   int documentsWritten, int sourcesDeleted) {
}
