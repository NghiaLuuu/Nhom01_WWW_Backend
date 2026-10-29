package vn.edu.iuh.nhom01_www.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionReport;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionService;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchRequest;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchResult;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchService;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatbotService;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatbotServiceTest {

    private ChatModel chatModel;
    private VectorSearchService vectorSearchService;
    private VectorIngestionService vectorIngestionService;
    private TripRepository tripRepository;
    private RouteRepository routeRepository;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        vectorSearchService = mock(VectorSearchService.class);
        vectorIngestionService = mock(VectorIngestionService.class);
        tripRepository = mock(TripRepository.class);
        routeRepository = mock(RouteRepository.class);

        chatbotService = new ChatbotService(
                chatModel,
                Optional.of(vectorSearchService),
                Optional.of(vectorIngestionService),
                tripRepository,
                routeRepository
        );
    }

    @Test
    void respondsToGreetingWithoutSearchingVectors() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("Xin chào");

        Map<String, Object> result = chatbotService.ask(request, "khach@gmail.com");

        assertThat(result).containsKey("answer");
        assertThat((String) result.get("answer")).contains("Xin chào", "VEXE");
        verifyNoInteractions(vectorSearchService);
        verifyNoInteractions(chatModel);
    }

    @Test
    void blocksMaliciousQuery() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("SELECT password FROM users; DROP TABLE routes;");

        Map<String, Object> result = chatbotService.ask(request, "attacker@gmail.com");

        assertThat((String) result.get("answer")).contains("Xin lỗi, tôi chỉ có thể hỗ trợ các thông tin liên quan");
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    void searchesVectorStoreAndGeneratesRagAnswer() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("Tìm xe đi Đà Lạt sáng mai");

        Route route = Route.builder().id(10L).departureLocation("TP.HCM").arrivalLocation("Đà Lạt")
                .distanceKm(new BigDecimal("300")).durationMinutes(360).basePrice(new BigDecimal("250000")).build();
        Vehicle vehicle = Vehicle.builder().id(5L).type("Limousine 34 phòng").capacity(34).build();
        Trip trip = Trip.builder().id(101L).route(route).vehicle(vehicle)
                .departureTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0))
                .price(new BigDecimal("350000")).status("PENDING").build();

        VectorSearchResult searchResult = new VectorSearchResult(
                "trip:101:0",
                "Thông tin chuyến xe: TP.HCM đến Đà Lạt",
                Map.of("sourceType", "trip", "tripId", "101", "routeId", "10"),
                0.88d
        );

        when(vectorSearchService.search(any(VectorSearchRequest.class))).thenReturn(List.of(searchResult));
        when(tripRepository.findById(101L)).thenReturn(Optional.of(trip));
        when(chatModel.call(any(String.class))).thenReturn("Dạ chào bạn! Hiện có chuyến Limousine đi Đà Lạt khởi hành lúc 08:00 với giá **350.000 VNĐ**.");

        Map<String, Object> response = chatbotService.ask(request, "user@gmail.com");

        assertThat(response).containsEntry("matchedCount", 1);
        assertThat((String) response.get("answer")).contains("350.000 VNĐ", "Đà Lạt");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(promptCaptor.capture());
        String generatedPrompt = promptCaptor.getValue();

        assertThat(generatedPrompt)
                .contains("TP.HCM -> Đà Lạt")
                .contains("Limousine 34 phòng")
                .contains("350.000 VNĐ")
                .contains("Tìm xe đi Đà Lạt sáng mai");
    }

    @Test
    void handlesEmptySearchResultsPolitely() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("Có xe đi Mù Cang Chải không?");

        when(vectorSearchService.search(any(VectorSearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(any(String.class))).thenReturn("Rất tiếc hiện tại VEXE chưa có chuyến đến Mù Cang Chải ạ. Bạn có thể tham khảo tuyến Hà Nội - Sapa nhé!");

        Map<String, Object> response = chatbotService.ask(request, "user@gmail.com");

        assertThat(response).containsEntry("matchedCount", 0);
        assertThat((String) response.get("answer")).contains("Mù Cang Chải");
    }

    @Test
    void ingestAllDelegatesToVectorIngestionService() {
        VectorIngestionReport report = new VectorIngestionReport(10, 0, 10, 0, 15, 0);
        when(vectorIngestionService.ingestAll()).thenReturn(report);

        VectorIngestionReport actual = chatbotService.ingestAll();

        assertThat(actual.sourcesRead()).isEqualTo(10);
        assertThat(actual.documentsWritten()).isEqualTo(15);
        verify(vectorIngestionService).ingestAll();
    }
}
