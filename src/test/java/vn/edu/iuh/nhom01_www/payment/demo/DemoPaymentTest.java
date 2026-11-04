package vn.edu.iuh.nhom01_www.payment.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.iuh.nhom01_www.aivector.VietnameseLocationNormalizer;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.AvailableSeatDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingConfirmationResultDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingDraftDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.TripSearchResultDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingAssistantService;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingDraftManager;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingIntentParser;
import vn.edu.iuh.nhom01_www.chatbot.booking.tool.BookingTools;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;
import vn.edu.iuh.nhom01_www.payment.demo.processor.DemoPaymentProcessor;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoPaymentTest {

    private BookingRepository bookingRepository;
    private PaymentRepository paymentRepository;
    private TicketRepository ticketRepository;
    private SeatHoldRedisService seatHoldRedisService;
    private TripRepository tripRepository;
    private RouteRepository routeRepository;
    private SeatRepository seatRepository;
    private BookingService bookingService;
    private TripBookingAvailabilityService tripBookingAvailabilityService;
    private CurrentUserService currentUserService;
    private ChatModel chatModel;

    private DemoPaymentProcessor demoPaymentProcessor;
    private BookingDraftManager draftManager;
    private BookingTools bookingTools;
    private BookingIntentParser intentParser;
    private BookingAssistantService assistantService;

    private Customer mockCustomer;
    private Customer mockOtherCustomer;
    private Route mockRoute;
    private Vehicle mockVehicle;
    private Trip mockTrip;
    private Seat seatA01;
    private Seat seatA02;
    private Seat seatA04;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        ticketRepository = mock(TicketRepository.class);
        seatHoldRedisService = mock(SeatHoldRedisService.class);
        tripRepository = mock(TripRepository.class);
        routeRepository = mock(RouteRepository.class);
        seatRepository = mock(SeatRepository.class);
        bookingService = mock(BookingService.class);
        tripBookingAvailabilityService = mock(TripBookingAvailabilityService.class);
        currentUserService = mock(CurrentUserService.class);
        chatModel = mock(ChatModel.class);

        draftManager = new BookingDraftManager(Optional.empty());

        demoPaymentProcessor = new DemoPaymentProcessor(
                bookingRepository,
                paymentRepository,
                ticketRepository,
                seatHoldRedisService
        );
        ReflectionTestUtils.setField(demoPaymentProcessor, "demoPaymentEnabled", true);

        bookingTools = new BookingTools(
                tripRepository,
                routeRepository,
                seatRepository,
                ticketRepository,
                bookingService,
                tripBookingAvailabilityService,
                seatHoldRedisService,
                currentUserService,
                draftManager,
                Optional.empty(),
                Optional.of(demoPaymentProcessor)
        );

        intentParser = new BookingIntentParser();

        assistantService = new BookingAssistantService(
                chatModel,
                bookingTools,
                draftManager,
                currentUserService,
                intentParser
        );

        mockCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .email("nguyenvana@gmail.com")
                .phoneNumber("0912345678")
                .build();

        mockOtherCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .fullName("Tran Van B")
                .email("tranvanb@gmail.com")
                .build();

        mockRoute = Route.builder()
                .id(1L)
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .basePrice(new BigDecimal("250000"))
                .build();

        mockVehicle = Vehicle.builder()
                .id(10L)
                .licensePlate("51B-99999")
                .type("Limousine")
                .capacity(34)
                .build();

        seatA01 = Seat.builder().id(101L).seatName("A01").floor(1).vehicle(mockVehicle).build();
        seatA02 = Seat.builder().id(102L).seatName("A02").floor(1).vehicle(mockVehicle).build();
        seatA04 = Seat.builder().id(104L).seatName("A04").floor(1).vehicle(mockVehicle).build();

        mockTrip = Trip.builder()
                .id(2001L)
                .route(mockRoute)
                .vehicle(mockVehicle)
                .departureTime(LocalDateTime.now().plusDays(1).withHour(21).withMinute(0))
                .price(new BigDecimal("250000"))
                .status("ACTIVE")
                .build();

        mockBooking = Booking.builder()
                .id(501L)
                .customer(mockCustomer)
                .trip(mockTrip)
                .totalPrice(new BigDecimal("500000"))
                .status("PENDING_PAYMENT")
                .createdAt(LocalDateTime.now())
                .tickets(new ArrayList<>())
                .build();

        Ticket ticket1 = Ticket.builder().id(701L).ticketCode("BK501DEMO").booking(mockBooking).seat(seatA01).price(new BigDecimal("250000")).status("PENDING").build();
        Ticket ticket2 = Ticket.builder().id(702L).ticketCode("BK501DEMO-2").booking(mockBooking).seat(seatA02).price(new BigDecimal("250000")).status("PENDING").build();
        mockBooking.getTickets().add(ticket1);
        mockBooking.getTickets().add(ticket2);

        when(currentUserService.getOptionalCurrentCustomer()).thenReturn(Optional.of(mockCustomer));
        when(tripRepository.findById(2001L)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.findAll()).thenReturn(List.of(mockTrip));
        when(seatRepository.findByVehicleId(10L)).thenReturn(List.of(seatA01, seatA02, seatA04));
        when(seatRepository.findAllById(any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return List.of(seatA01, seatA02, seatA04).stream()
                    .filter(s -> ids != null && ids.contains(s.getId()))
                    .toList();
        });
        when(ticketRepository.findBookedSeatIdsByTripId(2001L)).thenReturn(Collections.emptyList());
        when(seatHoldRedisService.getHeldSeatIdsByTripId(2001L)).thenReturn(Collections.emptyList());
        when(seatHoldRedisService.isBookingHoldActive(501L)).thenReturn(true);
        when(tripBookingAvailabilityService.getAvailableSeatCount(mockTrip)).thenReturn(32);
        when(tripBookingAvailabilityService.isWebBookingOpen(mockTrip)).thenReturn(true);
        when(bookingRepository.findById(501L)).thenReturn(Optional.of(mockBooking));
        when(bookingService.getBookingById(eq(501L), any())).thenReturn(mockBooking);
    }

    @Test
    @DisplayName("Scenario 1: Tìm chuyến xe thành công")
    void test1_SearchTripSuccess() {
        List<TripSearchResultDto> trips = bookingTools.searchAvailableTrips("Sài Gòn", "Đà Lạt", null, null, null);
        assertThat(trips).isNotEmpty();
        TripSearchResultDto trip = trips.get(0);
        assertThat(trip.getDepartureLocation()).isEqualTo("TP. Hồ Chí Minh");
        assertThat(trip.getArrivalLocation()).isEqualTo("Đà Lạt");
        assertThat(trip.getAvailableSeats()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Scenario 2: Không tìm thấy chuyến phù hợp")
    void test2_NoMatchingTrip() {
        List<TripSearchResultDto> trips = bookingTools.searchAvailableTrips("Hà Giang", "Cà Mau", null, null, null);
        assertThat(trips).isEmpty();
    }

    @Test
    @DisplayName("Scenario 3: Thiếu điểm đi — Tự động tra cứu danh sách điểm khởi hành phù hợp")
    void test3_MissingDeparturePrompt() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("Đặt giúp tôi 2 vé đi Đà Lạt tối mai");

        Map<String, Object> response = assistantService.assistBooking(request, mockCustomer.getEmail());
        assertThat(response.get("status")).isEqualTo("DEPARTURE_SELECTION_REQUIRED");
        assertThat(response.get("cardType")).isEqualTo("DEPARTURE_SELECTION");
        assertThat(response.get("departureOptions")).isNotNull();
    }

    @Test
    @DisplayName("Scenario 4: Nhận diện Sài Gòn = TP.HCM")
    void test4_SaiGonEqualsTpHcmRecognition() {
        VietnameseLocationNormalizer normalizer = new VietnameseLocationNormalizer();
        String normalizedSaiGon = normalizer.normalize("Sài Gòn");
        String normalizedTpHcm = normalizer.normalize("TP. Hồ Chí Minh");
        assertThat(normalizedSaiGon).isEqualTo("tp hcm");
        assertThat(normalizedTpHcm).isEqualTo("tp hcm");

        List<TripSearchResultDto> trips = bookingTools.searchAvailableTrips("Sài Gòn", "Đà Lạt", null, null, null);
        assertThat(trips).isNotEmpty();
    }

    @Test
    @DisplayName("Scenario 5: Đề xuất 2 ghế cạnh nhau thành công")
    void test5_SuggestAdjacentSeatsSuccess() {
        List<AvailableSeatDto> adjacent = bookingTools.suggestAdjacentSeats(2001L, 2, 1);
        assertThat(adjacent).hasSize(2);
        assertThat(adjacent.get(0).getSeatName()).isEqualTo("A01");
        assertThat(adjacent.get(1).getSeatName()).isEqualTo("A02");
    }

    @Test
    @DisplayName("Scenario 6: Không đủ 2 ghế cạnh nhau")
    void test6_NotEnoughAdjacentSeats() {
        // Only A01 and A04 are available (gap between 1 and 4)
        when(seatHoldRedisService.getHeldSeatIdsByTripId(2001L)).thenReturn(List.of(102L));
        List<AvailableSeatDto> adjacent = bookingTools.suggestAdjacentSeats(2001L, 2, 1);
        assertThat(adjacent).isEmpty();
    }

    @Test
    @DisplayName("Scenario 7: Xác nhận đặt vé thành công chuyển sang PENDING_PAYMENT")
    void test7_ConfirmBookingSuccess() {
        when(bookingService.createBooking(eq(mockCustomer.getId()), eq(2001L), eq(List.of(101L, 102L))))
                .thenReturn(mockBooking);

        BookingConfirmationResultDto result = bookingTools.confirmBooking(2001L, List.of(101L, 102L), "idemp-key-7");
        assertThat(result.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(result.getBookingId()).isEqualTo(501L);
        assertThat(result.getBookingCode()).isEqualTo("BK501DEMO");
    }

    @Test
    @DisplayName("Scenario 8: Hủy đặt vé")
    void test8_CancelBooking() {
        BookingDraftDto draft = bookingTools.prepareBooking(2001L, List.of(101L));
        assertThat(draftManager.getDraft(mockCustomer.getId(), draft.getIdempotencyKey())).isPresent();

        ChatbotRequest cancelRequest = new ChatbotRequest();
        cancelRequest.setQuestion("Hủy đặt vé");
        Map<String, Object> res = assistantService.assistBooking(cancelRequest, mockCustomer.getEmail());

        assertThat(res.get("status")).isEqualTo("REJECTED");
        assertThat((String) res.get("answer")).contains("đã hủy yêu cầu");
    }

    @Test
    @DisplayName("Scenario 9: Sinh mã QR demo thành công")
    void test9_GenerateDemoQrSuccess() {
        DemoPaymentQrResponse qrResponse = demoPaymentProcessor.createDemoPayment(501L, mockCustomer.getId());
        assertThat(qrResponse).isNotNull();
        assertThat(qrResponse.getBookingId()).isEqualTo(501L);
        assertThat(qrResponse.getStatus()).isEqualTo("PENDING");
        assertThat(qrResponse.getQrBase64()).startsWith("data:image/png;base64,");
        assertThat(qrResponse.getDemoNote()).contains("THANH TOÁN MÔ PHỎNG");
    }

    @Test
    @DisplayName("Scenario 10: Mô phỏng thanh toán thành công chuyển sang SIMULATED_PAID và phát hành vé")
    void test10_SimulatedPaymentSuccess() {
        when(paymentRepository.findByBookingId(501L)).thenReturn(Optional.of(
                Payment.builder().booking(mockBooking).status("PENDING").amount(new BigDecimal("500000")).build()
        ));

        DemoPaymentConfirmResponse confirm = demoPaymentProcessor.processPayment(501L, "DEMO_TXN_TEST", mockCustomer.getId());
        assertThat(confirm.isSuccess()).isTrue();
        assertThat(confirm.getStatus()).isEqualTo("PAID");
        assertThat(confirm.getTickets()).hasSize(2);
        assertThat(confirm.getTickets().get(0).getStatus()).isEqualTo("ISSUED");
        assertThat(mockBooking.getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Scenario 11: Thanh toán trùng lặp (Idempotent retry)")
    void test11_DuplicatePaymentIdempotent() {
        mockBooking.setStatus("PAID");
        DemoPaymentConfirmResponse confirm = demoPaymentProcessor.processPayment(501L, "DEMO_TXN_RETRY", mockCustomer.getId());
        assertThat(confirm.isSuccess()).isTrue();
        assertThat(confirm.getMessage()).contains("trước đó");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 12: Đặt vé hết hạn giữ chỗ bị từ chối")
    void test12_ExpiredBookingRejection() {
        when(seatHoldRedisService.isBookingHoldActive(501L)).thenReturn(false);
        assertThatThrownBy(() -> demoPaymentProcessor.createDemoPayment(501L, mockCustomer.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getStatus()).isEqualTo(HttpStatus.GONE);
                });
    }

    @Test
    @DisplayName("Scenario 13: Xung đột đặt trùng ghế")
    void test13_ConcurrentSeatConflict() {
        when(seatHoldRedisService.getHeldSeatIdsByTripId(2001L)).thenReturn(List.of(101L));
        List<AvailableSeatDto> available = bookingTools.getAvailableSeats(2001L);
        assertThat(available.stream().map(AvailableSeatDto::getSeatId)).doesNotContain(101L);
    }

    @Test
    @DisplayName("Scenario 14: Không có quyền thanh toán booking của người khác")
    void test14_UnauthorizedUserPaymentRejection() {
        assertThatThrownBy(() -> demoPaymentProcessor.createDemoPayment(501L, mockOtherCustomer.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Scenario 15: AI không tự ý confirm khi chưa có hành động người dùng")
    void test15_AiCannotAutoConfirmWithoutUserAction() {
        ChatbotRequest request = new ChatbotRequest();
        request.setQuestion("Tìm giúp tôi chuyến xe đi Đà Lạt ngày mai");
        assistantService.assistBooking(request, mockCustomer.getEmail());

        // Strictly verify createBooking was NEVER invoked
        verify(bookingService, never()).createBooking(any(), any(), any());
    }

    @Test
    @DisplayName("Scenario 16: Phân biệt mã QR vé điện tử và mã QR demo")
    void test16_TicketQrDistinctFromPaymentQr() {
        when(paymentRepository.findByBookingId(501L)).thenReturn(Optional.of(
                Payment.builder().booking(mockBooking).status("PENDING").amount(new BigDecimal("500000")).build()
        ));

        DemoPaymentQrResponse paymentQr = demoPaymentProcessor.createDemoPayment(501L, mockCustomer.getId());
        DemoPaymentConfirmResponse ticketConfirm = demoPaymentProcessor.processPayment(501L, "DEMO_TXN_DISTINCT", mockCustomer.getId());

        // Verify Demo Payment QR contains internal simulation tags
        assertThat(paymentQr.getQrPayload()).contains("DEMO_PAYMENT", "INTERNAL_DEMO_NO_REAL_MONEY");
        assertThat(paymentQr.getQrPayload()).doesNotContain("TICKET:");

        // Verify Ticket QR contains ticket data
        String ticketQrPayload = ticketConfirm.getTickets().get(0).getQrPayload();
        assertThat(ticketQrPayload).contains("TICKET:");
        assertThat(ticketQrPayload).doesNotContain("DEMO_PAYMENT");
    }

    @Test
    @DisplayName("Scenario 17: Phục hồi trạng thái đơn đặt vé khi reload")
    void test17_FrontendRefreshPreservesBookingState() {
        Map<String, Object> status = bookingTools.getBookingStatus(501L);
        assertThat(status.get("bookingId")).isEqualTo(501L);
        assertThat(status.get("bookingCode")).isEqualTo("BK501DEMO");
        assertThat(status.get("status")).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    @DisplayName("Scenario 18: Production chặn endpoint demo khi app.demo-payment.enabled=false")
    void test18_ProductionRejectionWhenDisabled() {
        ReflectionTestUtils.setField(demoPaymentProcessor, "demoPaymentEnabled", false);

        assertThatThrownBy(() -> demoPaymentProcessor.createDemoPayment(501L, mockCustomer.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ce.getMessage()).contains("bị vô hiệu hóa");
                });

        assertThatThrownBy(() -> demoPaymentProcessor.processPayment(501L, "TXN", mockCustomer.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ce = (CustomException) e;
                    assertThat(ce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
