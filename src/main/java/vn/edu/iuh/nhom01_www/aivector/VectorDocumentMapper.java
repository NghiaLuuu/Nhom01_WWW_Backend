package vn.edu.iuh.nhom01_www.aivector;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("ai-vector")
public class VectorDocumentMapper {

    private static final int SPLIT_THRESHOLD_CHARACTERS = 1_600;
    private static final DateTimeFormatter DEPARTURE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder().build();
    private final VietnameseLocationNormalizer locationNormalizer = new VietnameseLocationNormalizer();

    public Optional<VectorSourceDocument> map(Route route) {
        if (route == null || route.getId() == null || isBlank(route.getDepartureLocation()) || isBlank(route.getArrivalLocation())) {
            return Optional.empty();
        }

        StringBuilder content = new StringBuilder("Thông tin tuyến xe\n")
                .append("Mã tuyến nội bộ: ").append(route.getId()).append('\n')
                .append("Điểm đi: ").append(route.getDepartureLocation().trim()).append('\n')
                .append("Điểm đến: ").append(route.getArrivalLocation().trim());
        appendSearchAliases(content, route.getDepartureLocation(), route.getArrivalLocation());
        if (route.getDistanceKm() != null) {
            content.append("\nKhoảng cách: ").append(route.getDistanceKm().toPlainString()).append(" km");
        }
        if (route.getDurationMinutes() != null) {
            content.append("\nThời lượng dự kiến: ").append(route.getDurationMinutes()).append(" phút");
        }
        if (route.getStatus() != null) {
            content.append("\nTrạng thái tuyến: ").append(route.getStatus() ? "đang hoạt động" : "tạm dừng");
        }

        return source("route", route.getId(), content.toString(), Map.of(
                "routeId", route.getId().toString(),
                "departureLocation", route.getDepartureLocation().trim(),
                "arrivalLocation", route.getArrivalLocation().trim(),
                "departureLocationNormalized", locationNormalizer.normalize(route.getDepartureLocation()),
                "arrivalLocationNormalized", locationNormalizer.normalize(route.getArrivalLocation())));
    }

    public Optional<VectorSourceDocument> map(Trip trip) {
        if (trip == null || trip.getId() == null || trip.getDepartureTime() == null || trip.getRoute() == null
                || trip.getRoute().getId() == null || isBlank(trip.getRoute().getDepartureLocation())
                || isBlank(trip.getRoute().getArrivalLocation())) {
            return Optional.empty();
        }

        StringBuilder content = new StringBuilder("Thông tin chuyến xe\n")
                .append("Mã chuyến nội bộ: ").append(trip.getId()).append('\n')
                .append("Tuyến: ").append(trip.getRoute().getDepartureLocation().trim())
                .append(" đến ").append(trip.getRoute().getArrivalLocation().trim()).append('\n')
                .append("Thời gian khởi hành: ").append(DEPARTURE_TIME_FORMAT.format(trip.getDepartureTime()));
        appendSearchAliases(content, trip.getRoute().getDepartureLocation(), trip.getRoute().getArrivalLocation());
        if (trip.getVehicle() != null && !isBlank(trip.getVehicle().getType())) {
            content.append("\nLoại xe: ").append(trip.getVehicle().getType().trim());
        }
        if (trip.getStatus() != null) {
            content.append("\nTrạng thái chuyến: ").append(trip.getStatus());
        }

        Map<String, String> identifiers = new LinkedHashMap<>();
        identifiers.put("tripId", trip.getId().toString());
        identifiers.put("routeId", trip.getRoute().getId().toString());
        identifiers.put("departureLocationNormalized", locationNormalizer.normalize(trip.getRoute().getDepartureLocation()));
        identifiers.put("arrivalLocationNormalized", locationNormalizer.normalize(trip.getRoute().getArrivalLocation()));
        if (trip.getVehicle() != null && trip.getVehicle().getId() != null) {
            identifiers.put("vehicleId", trip.getVehicle().getId().toString());
        }
        return source("trip", trip.getId(), content.toString(), identifiers);
    }

    public Optional<VectorSourceDocument> map(Vehicle vehicle) {
        if (vehicle == null || vehicle.getId() == null || (isBlank(vehicle.getType()) && vehicle.getCapacity() == null)) {
            return Optional.empty();
        }

        StringBuilder content = new StringBuilder("Thông tin phương tiện\n")
                .append("Mã xe nội bộ: ").append(vehicle.getId());
        if (!isBlank(vehicle.getType())) {
            content.append("\nLoại xe: ").append(vehicle.getType().trim());
        }
        if (vehicle.getCapacity() != null) {
            content.append("\nSức chứa thiết kế: ").append(vehicle.getCapacity()).append(" chỗ");
        }
        if (!isBlank(vehicle.getStatus())) {
            content.append("\nTrạng thái phương tiện: ").append(vehicle.getStatus().trim());
        }

        return source("vehicle", vehicle.getId(), content.toString(), Map.of("vehicleId", vehicle.getId().toString()));
    }

    private Optional<VectorSourceDocument> source(String sourceType, Long sourceId, String content, Map<String, String> identifiers) {
        String normalizedContent = content.trim();
        if (normalizedContent.isEmpty()) {
            return Optional.empty();
        }
        String id = sourceId.toString();
        String version = sha256(normalizedContent);
        List<String> chunks = split(normalizedContent);
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceType", sourceType);
            metadata.put("sourceId", id);
            metadata.put("documentVersion", version);
            metadata.put("sourceKey", sourceType + ":" + id);
            metadata.put("chunkIndex", index);
            metadata.put("chunkCount", chunks.size());
            metadata.putAll(identifiers);
            documents.add(Document.builder()
                    .id(stableDocumentId(sourceType, id, index))
                    .text(chunks.get(index))
                    .metadata(metadata)
                    .build());
        }
        return Optional.of(new VectorSourceDocument(sourceType, id, version, List.copyOf(documents)));
    }

    private void appendSearchAliases(StringBuilder content, String departure, String arrival) {
        String normalizedDeparture = locationNormalizer.normalize(departure);
        String normalizedArrival = locationNormalizer.normalize(arrival);
        if (!normalizedDeparture.equals(locationNormalizer.normalize("TP.HCM"))) {
            return;
        }
        content.append("\nTên gọi tương đương điểm đi: Sài Gòn, Thành phố Hồ Chí Minh");
        if (normalizedArrival.equals(locationNormalizer.normalize("Đà Lạt"))) {
            content.append("\nÝ định tìm kiếm tuyến: từ Sài Gòn đến Đà Lạt");
        }
    }

    private List<String> split(String content) {
        if (content.length() <= SPLIT_THRESHOLD_CHARACTERS) {
            return List.of(content);
        }
        List<Document> splitDocuments = textSplitter.split(Document.builder().text(content).build());
        return splitDocuments.stream().map(Document::getText).filter(text -> !isBlank(text)).toList();
    }

    static String stableDocumentId(String sourceType, String sourceId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((sourceType + ":" + sourceId + ":" + chunkIndex)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte valueByte : hash) {
                result.append(String.format(Locale.ROOT, "%02x", valueByte));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
