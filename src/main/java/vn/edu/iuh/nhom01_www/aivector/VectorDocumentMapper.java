package vn.edu.iuh.nhom01_www.aivector;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
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

        String depNorm = locationNormalizer.normalize(route.getDepartureLocation());
        String arrNorm = locationNormalizer.normalize(route.getArrivalLocation());

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

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("routeId", route.getId().toString());
        meta.put("departureLocation", route.getDepartureLocation().trim());
        meta.put("arrivalLocation", route.getArrivalLocation().trim());
        meta.put("departureLocationNormalized", depNorm);
        meta.put("arrivalLocationNormalized", arrNorm);
        meta.put("corridor", resolveCorridor(depNorm, arrNorm));
        meta.put("departureRegion", resolveRegion(depNorm));
        meta.put("arrivalRegion", resolveRegion(arrNorm));

        return source("route", route.getId(), content.toString(), meta);
    }

    public Optional<VectorSourceDocument> map(Trip trip) {
        if (trip == null || trip.getId() == null || trip.getDepartureTime() == null || trip.getRoute() == null
                || trip.getRoute().getId() == null || isBlank(trip.getRoute().getDepartureLocation())
                || isBlank(trip.getRoute().getArrivalLocation())) {
            return Optional.empty();
        }

        String depNorm = locationNormalizer.normalize(trip.getRoute().getDepartureLocation());
        String arrNorm = locationNormalizer.normalize(trip.getRoute().getArrivalLocation());

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
        identifiers.put("departureLocationNormalized", depNorm);
        identifiers.put("arrivalLocationNormalized", arrNorm);
        identifiers.put("corridor", resolveCorridor(depNorm, arrNorm));
        identifiers.put("departureRegion", resolveRegion(depNorm));
        identifiers.put("arrivalRegion", resolveRegion(arrNorm));
        if (trip.getPrice() != null) {
            identifiers.put("priceCategory", trip.getPrice().compareTo(BigDecimal.valueOf(200000)) <= 0 ? "BUDGET"
                    : (trip.getPrice().compareTo(BigDecimal.valueOf(320000)) <= 0 ? "STANDARD" : "PREMIUM"));
        }
        if (trip.getVehicle() != null && trip.getVehicle().getId() != null) {
            identifiers.put("vehicleId", trip.getVehicle().getId().toString());
            if (trip.getVehicle().getType() != null) {
                identifiers.put("vehicleType", trip.getVehicle().getType());
            }
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

    private String resolveCorridor(String depNorm, String arrNorm) {
        String s1 = depNorm.replaceAll("[^a-z0-9]+", "_");
        String s2 = arrNorm.replaceAll("[^a-z0-9]+", "_");
        return (s1 + "_" + s2).toUpperCase(Locale.ROOT);
    }

    private String resolveRegion(String locationNorm) {
        if (locationNorm.contains("tp hcm") || locationNorm.contains("vung tau") || locationNorm.contains("can tho")) {
            return "SOUTH";
        }
        if (locationNorm.contains("phu yen") || locationNorm.contains("da lat") || locationNorm.contains("nha trang")
                || locationNorm.contains("da nang") || locationNorm.contains("hue") || locationNorm.contains("quy nhon")) {
            return "CENTRAL";
        }
        if (locationNorm.contains("ha noi") || locationNorm.contains("sapa") || locationNorm.contains("ha long")
                || locationNorm.contains("hai phong")) {
            return "NORTH";
        }
        return "GENERAL";
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
        if (normalizedDeparture.equals(locationNormalizer.normalize("TP.HCM"))) {
            content.append("\nTên gọi tương đương điểm đi: Sài Gòn, Thành phố Hồ Chí Minh");
            if (normalizedArrival.equals(locationNormalizer.normalize("Đà Lạt"))) {
                content.append("\nÝ định tìm kiếm tuyến: từ Sài Gòn đến Đà Lạt");
            } else if (normalizedArrival.equals(locationNormalizer.normalize("Phú Yên"))) {
                content.append("\nÝ định tìm kiếm tuyến: từ Sài Gòn về Phú Yên Tuy Hòa");
            }
        } else if (normalizedDeparture.equals(locationNormalizer.normalize("Phú Yên"))) {
            content.append("\nTên gọi tương đương điểm đi: Tuy Hòa, Tỉnh Phú Yên");
            if (normalizedArrival.equals(locationNormalizer.normalize("TP.HCM"))) {
                content.append("\nÝ định tìm kiếm tuyến: từ Phú Yên vào Sài Gòn TP.HCM");
            }
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
