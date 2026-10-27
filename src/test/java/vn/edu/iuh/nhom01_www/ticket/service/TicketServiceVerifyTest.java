package vn.edu.iuh.nhom01_www.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.dto.VerifyTicketRequest;
import vn.edu.iuh.nhom01_www.ticket.dto.VerifyTicketResponse;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.ticket.service.impl.TicketServiceImpl;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceVerifyTest {

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

    private Ticket ticket;
    private Booking booking;
    private Trip trip;
    private Staff staff;

    @BeforeEach
    void setUp() {
        Route route = Route.builder()
                .departureLocation("Ho Chi Minh City")
                .arrivalLocation("Da Lat")
                .build();
        Vehicle vehicle = Vehicle.builder().id(1L).build();
        trip = Trip.builder()
                .id(10L)
                .route(route)
                .vehicle(vehicle)
                .departureTime(LocalDate.now().atTime(12, 0))
                .status("SCHEDULED")
                .price(BigDecimal.valueOf(250_000))
                .build();

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .build();
        booking = Booking.builder()
                .id(100L)
                .customer(customer)
                .trip(trip)
                .status("PAID")
                .totalPrice(BigDecimal.valueOf(250_000))
                .build();
        Seat seat = Seat.builder()
                .id(5L)
                .seatName("A01")
                .vehicle(vehicle)
                .build();
        ticket = Ticket.builder()
                .id(1000L)
                .ticketCode("TK-123")
                .booking(booking)
                .seat(seat)
                .status("ISSUED")
                .price(BigDecimal.valueOf(250_000))
                .build();
        staff = Staff.builder()
                .id(UUID.randomUUID())
                .fullName("Staff One")
                .email("staff@example.com")
                .staffCode("ST001")
                .build();
    }

    @Test
    void verifyTicket_normalizesAsterisksAndAllWhitespace() {
        when(ticketRepository.findByTicketCode("TK-123")).thenReturn(Optional.of(ticket));
        when(currentUserService.getCurrentStaff()).thenReturn(staff);
        when(ticketRepository.markUsedIfUnused(eq(1000L), any(LocalDateTime.class), same(staff)))
                .thenReturn(1);

        VerifyTicketResponse response = ticketService.verifyTicket(
                VerifyTicketRequest.builder().code(" * T K- 1\t2\r\n3 * ").build());

        assertTrue(response.isValid());
        assertEquals("VERIFIED", response.getReason());
        assertNotNull(response.getTicket());
        assertEquals("TK-123", response.getTicket().getCode());
        assertEquals("Nguyen Van A", response.getTicket().getPassengerName());
        assertEquals("A01", response.getTicket().getSeat());
        assertEquals("Ho Chi Minh City - Da Lat", response.getTicket().getRoute());
        assertEquals(trip.getDepartureTime(), response.getTicket().getDepartureTime());
        verify(ticketRepository).markUsedIfUnused(eq(1000L), any(LocalDateTime.class), same(staff));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void verifyTicket_rejectsUnpaidBooking() {
        booking.setStatus("PENDING_PAYMENT");
        when(ticketRepository.findByTicketCode("TK-123")).thenReturn(Optional.of(ticket));

        VerifyTicketResponse response = ticketService.verifyTicket(
                VerifyTicketRequest.builder().code("TK-123").build());

        assertFalse(response.isValid());
        assertEquals("NOT_PAID", response.getReason());
        assertNotNull(response.getTicket());
        verify(ticketRepository, never()).markUsedIfUnused(any(), any(), any());
    }

    @Test
    void verifyTicket_rejectsWrongDepartureDate() {
        trip.setDepartureTime(LocalDate.now().plusDays(1).atTime(12, 0));
        when(ticketRepository.findByTicketCode("TK-123")).thenReturn(Optional.of(ticket));

        VerifyTicketResponse response = ticketService.verifyTicket(
                VerifyTicketRequest.builder().code("TK-123").build());

        assertFalse(response.isValid());
        assertEquals("WRONG_DEPARTURE_DATE", response.getReason());
        verify(ticketRepository, never()).markUsedIfUnused(any(), any(), any());
    }

    @Test
    void verifyTicket_rejectsAlreadyUsedTicket() {
        ticket.setStatus("USED");
        when(ticketRepository.findByTicketCode("TK-123")).thenReturn(Optional.of(ticket));

        VerifyTicketResponse response = ticketService.verifyTicket(
                VerifyTicketRequest.builder().code("TK-123").build());

        assertFalse(response.isValid());
        assertEquals("ALREADY_USED", response.getReason());
        verify(ticketRepository, never()).markUsedIfUnused(any(), any(), any());
    }

    @Test
    void verifyTicket_rejectsSecondScannerWhenAtomicUpdateLosesRace() {
        ticket.setStatus("UNUSED");
        when(ticketRepository.findByTicketCode("TK-123")).thenReturn(Optional.of(ticket));
        when(currentUserService.getCurrentStaff()).thenReturn(staff);
        when(ticketRepository.markUsedIfUnused(eq(1000L), any(LocalDateTime.class), same(staff)))
                .thenReturn(0);

        VerifyTicketResponse response = ticketService.verifyTicket(
                VerifyTicketRequest.builder().code("TK-123").build());

        assertFalse(response.isValid());
        assertEquals("ALREADY_USED", response.getReason());
        verify(ticketRepository).markUsedIfUnused(eq(1000L), any(LocalDateTime.class), same(staff));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }
}
