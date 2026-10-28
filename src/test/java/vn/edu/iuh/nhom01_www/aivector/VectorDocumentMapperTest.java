package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VectorDocumentMapperTest {

    private final VectorDocumentMapper mapper = new VectorDocumentMapper();

    @Test
    void mapsRouteWithStableIdAndSafeMetadataWithoutFare() {
        Route route = Route.builder()
                .id(7L)
                .departureLocation("TP.HCM")
                .arrivalLocation("Đà Lạt")
                .distanceKm(new BigDecimal("305.5"))
                .durationMinutes(420)
                .basePrice(new BigDecimal("250000"))
                .status(true)
                .build();

        VectorSourceDocument source = mapper.map(route).orElseThrow();
        Document document = source.documents().get(0);

        assertThat(document.getText()).contains("Điểm đi: TP.HCM", "Điểm đến: Đà Lạt", "Khoảng cách: 305.5 km")
                .contains("Sài Gòn", "Ý định tìm kiếm tuyến: từ Sài Gòn đến Đà Lạt")
                .doesNotContain("250000");
        assertThat(document.getMetadata()).containsEntry("sourceType", "route")
                .containsEntry("sourceId", "7")
                .containsEntry("routeId", "7")
                .containsEntry("departureLocationNormalized", "tp hcm")
                .containsEntry("arrivalLocationNormalized", "da lat")
                .containsEntry("chunkIndex", 0)
                .containsEntry("chunkCount", 1);
        assertThat(document.getId()).isEqualTo(VectorDocumentMapper.stableDocumentId("route", "7", 0));
        assertThat(source.documentVersion()).hasSize(64);
    }

    @Test
    void mapsTripOnlyFromExistingNonSensitiveFields() {
        Route route = Route.builder().id(2L).departureLocation("Huế").arrivalLocation("Đà Nẵng").build();
        Vehicle vehicle = Vehicle.builder().id(3L).type("Giường nằm").capacity(40).licensePlate("51B-123.45").build();
        Trip trip = Trip.builder().id(9L).route(route).vehicle(vehicle)
                .departureTime(LocalDateTime.of(2026, 11, 1, 8, 30)).price(new BigDecimal("300000"))
                .status("PENDING").build();

        Document document = mapper.map(trip).orElseThrow().documents().get(0);

        assertThat(document.getText()).contains("Tuyến: Huế đến Đà Nẵng", "Loại xe: Giường nằm")
                .doesNotContain("300000", "51B-123.45");
        assertThat(document.getMetadata()).containsEntry("tripId", "9")
                .containsEntry("routeId", "2")
                .containsEntry("departureLocationNormalized", "hue")
                .containsEntry("arrivalLocationNormalized", "da nang")
                .containsEntry("vehicleId", "3");
    }

    @Test
    void skipsInvalidRecords() {
        assertThat(mapper.map(Route.builder().id(1L).departureLocation(" ").arrivalLocation("Huế").build())).isEmpty();
        assertThat(mapper.map(Trip.builder().id(1L).build())).isEmpty();
        assertThat(mapper.map(Vehicle.builder().id(1L).build())).isEmpty();
    }

    @Test
    void ingestionComponentsRequireAiVectorProfile() {
        assertThat(VectorIngestionService.class.getAnnotation(Profile.class).value()).containsExactly("ai-vector");
        assertThat(VectorDocumentMapper.class.getAnnotation(Profile.class).value()).containsExactly("ai-vector");
    }
}
