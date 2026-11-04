package vn.edu.iuh.nhom01_www.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.DepartureOptionDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.TripSearchResultDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingAssistantService;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingIntentParser;
import vn.edu.iuh.nhom01_www.chatbot.booking.tool.BookingTools;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.MyBookingSummaryDto;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.service.CancellationAssistantService;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatIntent;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatIntentClassifier;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatbotService;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotIntentRoutingTest {

    @Mock private ChatModel chatModel;
    @Mock private TripRepository tripRepository;
    @Mock private RouteRepository routeRepository;
    @Mock private BookingAssistantService bookingAssistantService;
    @Mock private CancellationAssistantService cancellationAssistantService;
    @Mock private BookingTools bookingTools;

    private ChatIntentClassifier intentClassifier;
    private ChatbotService chatbotService;

    private Route routeHcmDalat;
    private Trip tripTomorrow;

    @BeforeEach
    void setUp() {
        intentClassifier = new ChatIntentClassifier();
        chatbotService = new ChatbotService(
                chatModel,
                Optional.empty(),
                Optional.empty(),
                tripRepository,
                routeRepository,
                Optional.of(bookingAssistantService),
                Optional.of(cancellationAssistantService),
                intentClassifier
        );

        routeHcmDalat = Route.builder()
                .id(1L)
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .build();

        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        tripTomorrow = Trip.builder()
                .id(101L)
                .route(routeHcmDalat)
                .departureTime(tomorrow.atTime(8, 30))
                .price(new BigDecimal("250000"))
                .status("ACTIVE")
                .vehicle(Vehicle.builder().id(1L).type("Giường nằm").capacity(34).build())
                .build();
    }

    @Test
    @DisplayName("1. 'Các chuyến xe khởi hành vào ngày mai' -> Phân loại SEARCH_TRIPS và hiển thị danh sách chuyến ngày mai")
    void test1_SearchTripsTomorrow() {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        when(tripRepository.findAll()).thenReturn(List.of(tripTomorrow));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Các chuyến xe khởi hành vào ngày mai");

        Map<String, Object> res = chatbotService.ask(req, "test@example.com");

        assertThat(res.get("cardType")).isEqualTo("TRIP_LIST");
        assertThat(res.get("status")).isEqualTo("TRIPS_FOUND");
        String answer = (String) res.get("answer");
        assertThat(answer).contains("CÁC CHUYẾN XE KHỞI HÀNH");
        assertThat(answer).contains("Chuyến #101");
        assertThat(answer).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");
    }

    @Test
    @DisplayName("2. 'Xe Sài Gòn đi Đà Lạt' -> Phân loại SEARCH_TRIPS, tra cứu tuyến TP.HCM -> Đà Lạt, không hỏi lại điểm đi")
    void test2_SearchTripsSpecificRoute() {
        when(bookingAssistantService.getBookingTools()).thenReturn(bookingTools);

        TripSearchResultDto tripDto = TripSearchResultDto.builder()
                .tripId(101L)
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .departureTime(LocalDateTime.now().plusDays(1))
                .departureTimeFormatted("08:30 ngày mai")
                .price(new BigDecimal("250000"))
                .priceFormatted("250.000 VNĐ")
                .vehicleType("Giường nằm")
                .availableSeats(20)
                .build();

        when(bookingTools.searchAvailableTrips(eq("Sài Gòn"), eq("Đà Lạt"), any(), any(), any(), eq(1)))
                .thenReturn(List.of(tripDto));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Xe Sài Gòn đi Đà Lạt");

        Map<String, Object> res = chatbotService.ask(req, "test@example.com");

        assertThat(res.get("cardType")).isEqualTo("TRIP_LIST");
        assertThat(res.get("status")).isEqualTo("TRIPS_FOUND");
        String answer = (String) res.get("answer");
        assertThat(answer).contains("Sài Gòn");
        assertThat(answer).contains("Đà Lạt");
        assertThat(answer).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");
    }

    @Test
    @DisplayName("3. 'Xem đơn đặt vé của tôi' -> Phân loại VIEW_MY_BOOKINGS, hiển thị danh sách vé không hỏi điểm đi/đến")
    void test3_ViewMyBookings() {
        MyBookingSummaryDto bDto = MyBookingSummaryDto.builder()
                .bookingId(7632L)
                .bookingCode("BK7632")
                .routeName("TP. Hồ Chí Minh ➔ Đà Lạt")
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .departureTimeFormatted("20:00 ngày 21/11/2026")
                .seatNames(List.of("VIP8", "VIP2"))
                .ticketCount(2)
                .totalPriceFormatted("700.000 VNĐ")
                .status("PAID")
                .isCancellable(true)
                .build();

        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of(
                        "status", "BOOKINGS_FOUND",
                        "cardType", "BOOKING_LOOKUP",
                        "bookings", List.of(bDto),
                        "answer", "Danh sách vé của bạn: BK7632"
                ));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Xem đơn đặt vé của tôi");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("cardType")).isEqualTo("BOOKING_LOOKUP");
        assertThat(res.get("status")).isEqualTo("BOOKINGS_FOUND");
        assertThat((String) res.get("answer")).contains("BK7632");
        assertThat((String) res.get("answer")).doesNotContain("muốn khởi hành từ điểm nào");
    }

    @Test
    @DisplayName("4. 'Hủy vé đến Đà Lạt' -> Phân loại CANCEL_TICKET và ủy quyền cho CancellationAssistant")
    void test4_CancelTicketIntent() {
        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of(
                        "status", "PREVIEW_REQUIRED",
                        "cardType", "CANCELLATION_PREVIEW",
                        "answer", "Tóm tắt phí hủy vé đến Đà Lạt"
                ));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Hủy vé đến Đà Lạt");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("cardType")).isEqualTo("CANCELLATION_PREVIEW");
        verify(cancellationAssistantService).assistCancellation(req, "user@test.com");
    }

    @Test
    @DisplayName("5. 'Đặt 2 vé đến Đà Lạt ngày mai' thiếu điểm đi -> Phân loại BOOK_TICKET, hiển thị DepartureSelectionCard")
    void test5_BookTicketMissingDeparture() {
        DepartureOptionDto opt = DepartureOptionDto.builder()
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .activeTripCount(5)
                .totalAvailableSeats(45)
                .minPriceFormatted("250.000 VNĐ")
                .earliestDepartureTimeFormatted("06:30")
                .vehicleTypes(List.of("Giường nằm", "Limousine"))
                .command("SELECT_DEPARTURE:TP. Hồ Chí Minh")
                .build();

        when(bookingAssistantService.assistBooking(any(), eq("user@test.com")))
                .thenReturn(Map.of(
                        "status", "DEPARTURE_SELECTION_REQUIRED",
                        "cardType", "DEPARTURE_SELECTION",
                        "departureOptions", List.of(opt),
                        "answer", "Danh sách điểm khởi hành đến Đà Lạt"
                ));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Đặt 2 vé đến Đà Lạt ngày mai");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("cardType")).isEqualTo("DEPARTURE_SELECTION");
        assertThat(res.get("status")).isEqualTo("DEPARTURE_SELECTION_REQUIRED");
    }

    @Test
    @DisplayName("6. Chuyển từ BOOK_TICKET sang VIEW_MY_BOOKINGS -> Reset booking session, hiển thị danh sách vé")
    void test6_SwitchFromBookingToViewMyBookings() {
        when(bookingAssistantService.hasActiveBookingFlow(any())).thenReturn(true);
        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of("status", "BOOKINGS_FOUND", "cardType", "BOOKING_LOOKUP"));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Xem đơn đặt vé của tôi");
        req.setSessionId("session_123");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("status")).isEqualTo("BOOKINGS_FOUND");
        verify(bookingAssistantService).resetBookingSession("session_123");
        verify(cancellationAssistantService).assistCancellation(req, "user@test.com");
    }

    @Test
    @DisplayName("7. Chuyển từ BOOK_TICKET sang CANCEL_TICKET -> Reset booking session, gọi hủy vé")
    void test7_SwitchFromBookingToCancelTicket() {
        when(bookingAssistantService.hasActiveBookingFlow(any())).thenReturn(true);
        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of("status", "PREVIEW_REQUIRED", "cardType", "CANCELLATION_PREVIEW"));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Hủy vé đến Đà Lạt");
        req.setSessionId("session_123");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("status")).isEqualTo("PREVIEW_REQUIRED");
        verify(bookingAssistantService).resetBookingSession("session_123");
        verify(cancellationAssistantService).assistCancellation(req, "user@test.com");
    }

    @Test
    @DisplayName("8. Chuyển từ CANCEL_TICKET sang SEARCH_TRIPS -> Reset booking draft, tra cứu chuyến mới")
    void test8_SwitchFromCancelToSearchTrips() {
        when(tripRepository.findAll()).thenReturn(List.of(tripTomorrow));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Các chuyến xe khởi hành vào ngày mai");
        req.setSessionId("session_123");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("status")).isEqualTo("TRIPS_FOUND");
        verify(bookingAssistantService).resetBookingSession("session_123");
    }

    @Test
    @DisplayName("9. Không dùng bookingId từ session cũ khi bắt đầu tra cứu mới")
    void test9_DoNotUseBookingIdFromOldSession() {
        when(tripRepository.findAll()).thenReturn(List.of(tripTomorrow));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Các chuyến xe khởi hành vào ngày mai");
        req.setSessionId("session_old_123");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("status")).isEqualTo("TRIPS_FOUND");
        assertThat(res.get("bookingId")).isNull();
    }

    @Test
    @DisplayName("10. AI không trả lời lặp lại câu hỏi 'Dạ anh/chị muốn khởi hành từ điểm nào đến Đà Lạt ạ?'")
    void test10_AiDoesNotRepeatStaleQuestion() {
        when(tripRepository.findAll()).thenReturn(List.of(tripTomorrow));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Các chuyến xe khởi hành vào ngày mai");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        String answer = (String) res.get("answer");
        assertThat(answer).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");
    }

    @Test
    @DisplayName("11. Khách vãng lai ANONYMOUS yêu cầu xem vé -> Báo lỗi UNAUTHORIZED, không lộ dữ liệu người khác")
    void test11_AnonymousUserCannotViewBookings() {
        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Xem đơn đặt vé của tôi");

        Map<String, Object> res = chatbotService.ask(req, "ANONYMOUS");

        assertThat(res.get("status")).isEqualTo("UNAUTHORIZED");
        assertThat((String) res.get("answer")).contains("Yêu cầu đăng nhập");
        verify(cancellationAssistantService, never()).assistCancellation(any(), any());
    }

    @Test
    @DisplayName("12. Không mất trạng thái booking đã tạo khi chuyển intent (booking session reset bảo lưu DB record)")
    void test12_DoNotLoseDatabaseBookingOnIntentSwitch() {
        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of("status", "BOOKINGS_FOUND"));

        ChatbotRequest req = new ChatbotRequest();
        req.setQuestion("Xem đơn đặt vé của tôi");
        req.setSessionId("session_with_pending_booking");

        Map<String, Object> res = chatbotService.ask(req, "user@test.com");

        assertThat(res.get("status")).isEqualTo("BOOKINGS_FOUND");
        // Verifies resetBookingSession is called, which clears in-memory state but does not delete database records
        verify(bookingAssistantService).resetBookingSession("session_with_pending_booking");
    }

    @Test
    @DisplayName("13. Bot greeting trong history có chứa 'Đà Lạt' KHÔNG làm rò rỉ arrivalLocation vào user query mới")
    void test13_BotGreetingDoesNotLeakDalatIntoNewQuery() {
        BookingIntentParser parser = new BookingIntentParser();

        List<ChatbotRequest.ChatMessage> history = List.of(
                new ChatbotRequest.ChatMessage("assistant",
                        "**Xin chào!** Em là trợ lý AI thông minh của VEXE 🚌.\n*Ví dụ: \"Hủy giúp tôi 2 vé đi Đà Lạt tối mai\"*")
        );

        var intent = parser.parse("Các chuyến xe khởi hành vào ngày mai", history);

        // Crucial test: arrivalLocation must NOT be "Đà Lạt" because the text was from assistant, not user!
        assertThat(intent.getArrivalLocation()).isNull();
        assertThat(intent.getDepartureLocation()).isNull();
    }

    @Test
    @DisplayName("14. ACCEPTANCE TEST: 3 câu hỏi độc lập trong cùng 1 hội thoại -> Xử lý chính xác, không lặp lại câu hỏi cũ")
    void test14_AcceptanceTestThreeQueriesInSameConversation() {
        when(bookingAssistantService.getBookingTools()).thenReturn(bookingTools);

        // Setup TripRepository for Query 1
        when(tripRepository.findAll()).thenReturn(List.of(tripTomorrow));

        // Setup BookingTools for Query 2
        TripSearchResultDto tripHcmDalat = TripSearchResultDto.builder()
                .tripId(101L)
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .departureTime(LocalDateTime.now().plusDays(1))
                .departureTimeFormatted("08:30")
                .price(new BigDecimal("250000"))
                .priceFormatted("250.000 VNĐ")
                .vehicleType("Giường nằm")
                .availableSeats(20)
                .build();
        when(bookingTools.searchAvailableTrips(eq("Sài Gòn"), eq("Đà Lạt"), any(), any(), any(), eq(1)))
                .thenReturn(List.of(tripHcmDalat));

        // Setup CancellationAssistantService for Query 3
        when(cancellationAssistantService.assistCancellation(any(), eq("user@test.com")))
                .thenReturn(Map.of(
                        "status", "BOOKINGS_FOUND",
                        "cardType", "BOOKING_LOOKUP",
                        "answer", "Danh sách vé xe của bạn gồm BK7632"
                ));

        String sessionId = "shared_conversation_session_999";
        List<ChatbotRequest.ChatMessage> conversationHistory = new ArrayList<>();

        // Turn 1: "Các chuyến xe khởi hành vào ngày mai"
        ChatbotRequest req1 = new ChatbotRequest();
        req1.setQuestion("Các chuyến xe khởi hành vào ngày mai");
        req1.setSessionId(sessionId);
        req1.setHistory(conversationHistory);

        Map<String, Object> res1 = chatbotService.ask(req1, "user@test.com");
        assertThat(res1.get("status")).isEqualTo("TRIPS_FOUND");
        assertThat((String) res1.get("answer")).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");

        conversationHistory.add(new ChatbotRequest.ChatMessage("user", req1.getQuestion()));
        conversationHistory.add(new ChatbotRequest.ChatMessage("assistant", (String) res1.get("answer")));

        // Turn 2: "Xe Sài Gòn đi Đà Lạt"
        ChatbotRequest req2 = new ChatbotRequest();
        req2.setQuestion("Xe Sài Gòn đi Đà Lạt");
        req2.setSessionId(sessionId);
        req2.setHistory(conversationHistory);

        Map<String, Object> res2 = chatbotService.ask(req2, "user@test.com");
        assertThat(res2.get("status")).isEqualTo("TRIPS_FOUND");
        assertThat((String) res2.get("answer")).contains("Sài Gòn");
        assertThat((String) res2.get("answer")).contains("Đà Lạt");
        assertThat((String) res2.get("answer")).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");

        conversationHistory.add(new ChatbotRequest.ChatMessage("user", req2.getQuestion()));
        conversationHistory.add(new ChatbotRequest.ChatMessage("assistant", (String) res2.get("answer")));

        // Turn 3: "Xem đơn đặt vé của tôi"
        ChatbotRequest req3 = new ChatbotRequest();
        req3.setQuestion("Xem đơn đặt vé của tôi");
        req3.setSessionId(sessionId);
        req3.setHistory(conversationHistory);

        Map<String, Object> res3 = chatbotService.ask(req3, "user@test.com");
        assertThat(res3.get("status")).isEqualTo("BOOKINGS_FOUND");
        assertThat(res3.get("cardType")).isEqualTo("BOOKING_LOOKUP");
        assertThat((String) res3.get("answer")).contains("BK7632");
        assertThat((String) res3.get("answer")).doesNotContain("muốn khởi hành từ điểm nào đến Đà Lạt");
    }
}
