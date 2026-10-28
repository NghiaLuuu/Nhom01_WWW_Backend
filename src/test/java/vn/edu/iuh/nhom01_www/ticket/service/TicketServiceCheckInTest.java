package vn.edu.iuh.nhom01_www.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.dto.*;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.ticket.service.impl.TicketServiceImpl;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceCheckInTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatHoldRedisService seatHoldRedisService;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Ticket validTicket;
    private Trip trip;
    private Route route;
    private Vehicle vehicle;
    private Customer customer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketService, "checkInOpenMinutesBefore", 60);
        ReflectionTestUtils.setField(ticketService, "checkInCloseMinutesBefore", 10);
        ReflectionTestUtils.setField(ticketService, "rescheduleFeePercent", 20);
        ReflectionTestUtils.setField(ticketService, "minRescheduleFee", BigDecimal.valueOf(30000));

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyễn Văn A")
                .phoneNumber("0901234567")
                .build();

        vehicle = Vehicle.builder()
                .id(1L)
                .licensePlate("51B-12345")
                .type("VIP")
                .build();

        route = Route.builder()
                .id(1L)
                .departureLocation("TP. Hồ Chí Minh")
                .arrivalLocation("Đà Lạt")
                .build();

        trip = Trip.builder()
                .id(10L)
                .status("SCHEDULED")
                .departureTime(LocalDateTime.now().plusMinutes(30)) // In valid 60m - 10m window
                .vehicle(vehicle)
                .route(route)
                .price(BigDecimal.valueOf(250000))
                .build();

        Booking booking = Booking.builder()
                .id(100L)
                .trip(trip)
                .customer(customer)
                .build();

        Seat seat = Seat.builder()
                .id(5L)
                .seatName("A01")
                .floor(1)
                .vehicle(vehicle)
                .build();

        validTicket = Ticket.builder()
                .id(1000L)
                .ticketCode("TK-123456")
                .status("ISSUED")
                .price(BigDecimal.valueOf(250000))
                .seat(seat)
                .booking(booking)
                .build();
    }

    @Test
    @DisplayName("Soát vé thành công khi trong khung giờ hợp lệ (30 phút trước giờ chạy)")
    void checkIn_Success_WithinWindow() {
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .build();

        CheckInResponse response = ticketService.checkInTicket(request);

        assertNotNull(response);
        assertEquals("USED", response.getStatus());
        assertEquals("TK-123456", response.getTicketCode());
        assertEquals("A01", response.getSeatName());
        assertEquals("Nguyễn Văn A", response.getCustomerName());
        verify(ticketRepository).save(validTicket);
    }

    @Test
    @DisplayName("Từ chối soát vé khi đến quá sớm (trước khi mở cổng 60 phút)")
    void checkIn_Fails_TooEarly() {
        trip.setDepartureTime(LocalDateTime.now().plusMinutes(120));
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> ticketService.checkInTicket(request));
        assertTrue(exception.getMessage().contains("Chưa đến giờ soát vé"));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tự động chuyển vé sang NO_SHOW và từ chối khi cổng soát vé đã đóng (dưới 10 phút trước giờ xuất bến)")
    void checkIn_Fails_GateClosed_AndMarksNoShow() {
        // Departure is 5 minutes from now, but gate closed 10 minutes before departure
        trip.setDepartureTime(LocalDateTime.now().plusMinutes(5));
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> ticketService.checkInTicket(request));
        assertTrue(exception.getMessage().contains("Cổng soát vé đã ĐÓNG"));
        assertTrue(exception.getMessage().contains("NO-SHOW"));
        assertEquals("NO_SHOW", validTicket.getStatus());
        verify(ticketRepository).save(validTicket);
    }

    @Test
    @DisplayName("Tự động chuyển vé sang NO_SHOW và từ chối khi xe đã xuất bến")
    void checkIn_Fails_AfterDeparture_AndMarksNoShow() {
        // Bus departed 10 minutes ago
        trip.setDepartureTime(LocalDateTime.now().minusMinutes(10));
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> ticketService.checkInTicket(request));
        assertTrue(exception.getMessage().contains("Chuyến xe đã khởi hành"));
        assertTrue(exception.getMessage().contains("NO-SHOW"));
        assertEquals("NO_SHOW", validTicket.getStatus());
        verify(ticketRepository).save(validTicket);
    }

    @Test
    @DisplayName("Cho phép Nhân viên / Quản lý soát vé cưỡng chế (forceCheckIn) kể cả khi trễ hoặc cổng đã đóng")
    void checkIn_Success_ForceCheckIn_ByStaff() {
        trip.setDepartureTime(LocalDateTime.now().plusMinutes(2));
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));
        when(currentUserService.isStaffOrAdmin()).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .forceCheckIn(true)
                .build();

        CheckInResponse response = ticketService.checkInTicket(request);

        assertNotNull(response);
        assertEquals("USED", response.getStatus());
        verify(ticketRepository).save(validTicket);
    }

    @Test
    @DisplayName("Từ chối soát vé khi vé đã được check-in trước đó (chống gian lận vé trùng)")
    void checkIn_Fails_AlreadyUsed() {
        validTicket.setStatus("USED");
        validTicket.setUsedAt(LocalDateTime.now().minusMinutes(15));
        when(ticketRepository.findByTicketCode("TK-123456")).thenReturn(Optional.of(validTicket));

        CheckInRequest request = CheckInRequest.builder()
                .ticketCodeOrQr("TK-123456")
                .tripId(10L)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> ticketService.checkInTicket(request));
        assertTrue(exception.getMessage().contains("CẢNH BÁO: Vé này đã được check-in"));
        verify(ticketRepository, never()).save(any());
    }

    // ==================== KIỂM THỬ NGHIỆP VỤ NO-SHOW & CHÍNH SÁCH ====================

    @Test
    @DisplayName("Chốt danh sách xuất bến chuyến xe: chuyển các vé chưa lên xe sang NO_SHOW")
    void markTripNoShows_Success() {
        when(tripRepository.existsById(10L)).thenReturn(true);
        when(ticketRepository.findIssuedTicketsByTripId(10L)).thenReturn(List.of(validTicket));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        NoShowSummaryDto summary = ticketService.markTripNoShows(10L);

        assertNotNull(summary);
        assertEquals(1, summary.getTotalNoShowMarked());
        assertEquals("NO_SHOW", validTicket.getStatus());
        verify(ticketRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Chính sách 1: Hủy hoàn toàn vé No-Show (không hoàn tiền)")
    void cancelNoShowTicket_Success() {
        validTicket.setStatus("NO_SHOW");
        when(ticketRepository.findById(1000L)).thenReturn(Optional.of(validTicket));
        when(currentUserService.isStaffOrAdmin()).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.cancelNoShowTicket(1000L, "Khách không có mặt trước giờ chốt vé");

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
        verify(ticketRepository).save(validTicket);
    }

    @Test
    @DisplayName("Chính sách 2: Tra cứu các chuyến tiếp theo còn chỗ & ước tính phụ phí đổi vé")
    void getRescheduleOptions_Success() {
        when(ticketRepository.findById(1000L)).thenReturn(Optional.of(validTicket));
        when(currentUserService.isStaffOrAdmin()).thenReturn(true);

        Trip nextTrip = Trip.builder()
                .id(20L)
                .route(route)
                .vehicle(vehicle)
                .departureTime(LocalDateTime.now().plusHours(3))
                .price(BigDecimal.valueOf(300000))
                .status("SCHEDULED")
                .build();

        when(tripRepository.findAvailableNextTripsByRouteId(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(nextTrip));

        Seat seat1 = Seat.builder().id(11L).seatName("B01").vehicle(vehicle).build();
        when(seatRepository.findByVehicleId(1L)).thenReturn(List.of(seat1));
        when(ticketRepository.findBookedSeatIdsByTripId(20L)).thenReturn(List.of());
        when(seatHoldRedisService.getHeldSeatIdsByTripId(20L)).thenReturn(List.of());

        List<RescheduleOptionDto> options = ticketService.getRescheduleOptions(1000L);

        assertNotNull(options);
        assertEquals(1, options.size());
        RescheduleOptionDto option = options.get(0);
        assertEquals(20L, option.getTripId());
        assertEquals(BigDecimal.valueOf(250000), option.getOriginalPrice());
        // Phí đổi 20% của 250k = 50.000 VNĐ (> min 30k)
        assertEquals(BigDecimal.valueOf(50000), option.getRescheduleFee());
        // Chênh lệch giá chuyến mới: 300k - 250k = 50.000 VNĐ
        assertEquals(BigDecimal.valueOf(50000), option.getPriceDifference());
        // Tổng phụ thu: 50k + 50k = 100.000 VNĐ
        assertEquals(BigDecimal.valueOf(100000), option.getTotalAdditionalFee());
    }

    @Test
    @DisplayName("Chính sách 2: Đổi vé No-Show sang chuyến tiếp theo thành công (tạo booking & vé mới)")
    void rescheduleNoShowTicket_Success() {
        validTicket.setStatus("NO_SHOW");
        when(ticketRepository.findById(1000L)).thenReturn(Optional.of(validTicket));
        when(currentUserService.isStaffOrAdmin()).thenReturn(true);

        Trip nextTrip = Trip.builder()
                .id(20L)
                .route(route)
                .vehicle(vehicle)
                .departureTime(LocalDateTime.now().plusHours(3))
                .price(BigDecimal.valueOf(280000))
                .status("SCHEDULED")
                .build();

        when(tripRepository.findById(20L)).thenReturn(Optional.of(nextTrip));

        Seat seatB1 = Seat.builder().id(11L).seatName("B01").floor(1).vehicle(vehicle).build();
        when(seatRepository.findById(11L)).thenReturn(Optional.of(seatB1));
        when(seatRepository.findByVehicleId(1L)).thenReturn(List.of(seatB1));
        when(ticketRepository.findBookedSeatIdsByTripId(20L)).thenReturn(List.of());
        when(seatHoldRedisService.getHeldSeatIdsByTripId(20L)).thenReturn(List.of());

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(200L);
            return b;
        });

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(2000L);
            return t;
        });

        RescheduleTicketRequest request = RescheduleTicketRequest.builder()
                .newTripId(20L)
                .newSeatId(11L)
                .paymentMethod("CASH")
                .build();

        RescheduleTicketResponse response = ticketService.rescheduleNoShowTicket(1000L, request);

        assertNotNull(response);
        assertEquals(1000L, response.getOldTicketId());
        assertEquals("RESCHEDULED", validTicket.getStatus()); // Vé cũ được đánh dấu RESCHEDULED
        assertEquals(20L, response.getNewTripId());
        assertEquals("B01", response.getNewSeatName());
        assertEquals("ISSUED", response.getStatus()); // Vé mới sẵn sàng đi chuyến mới
        // Phí đổi 20% của 250k = 50k; Chênh lệch = 280k - 250k = 30k; Tổng = 80k
        assertEquals(BigDecimal.valueOf(50000), response.getRescheduleFee());
        assertEquals(BigDecimal.valueOf(30000), response.getPriceDifference());
        assertEquals(BigDecimal.valueOf(80000), response.getTotalAdditionalFee());

        verify(bookingRepository).save(any(Booking.class));
        verify(paymentRepository).save(any());
    }
}
