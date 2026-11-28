package vn.edu.iuh.nhom01_www.cancelrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.service.impl.CancelRequestServiceImpl;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelRequestServiceTest {
    @Mock CancelRequestRepository requests;
    @Mock BookingRepository bookings;
    @Mock TicketRepository tickets;
    @Mock CurrentUserService currentUser;
    @InjectMocks CancelRequestServiceImpl service;

    Customer customer;
    Staff staff;
    Booking booking;
    Ticket ticket;
    CancelRequest pending;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(UUID.randomUUID()).email("customer@example.com")
                .fullName("Khách hàng").password("never-serialize-customer-password").build();
        staff = Staff.builder().id(UUID.randomUUID()).fullName("Nhân viên")
                .staffCode("NV001").password("never-serialize-staff-password").build();
        var route = Route.builder().id(1L).departureLocation("TP.HCM").arrivalLocation("Đà Lạt").build();
        var trip = Trip.builder().id(10L).route(route).status("SCHEDULED")
                .departureTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2)).build();
        booking = Booking.builder().id(100L).customer(customer).trip(trip).status("PAID")
                .totalPrice(new BigDecimal("250000")).build();
        ticket = Ticket.builder().id(1000L).booking(booking).ticketCode("VX1000").status("ISSUED")
                .seat(Seat.builder().id(5L).seatName("A01").build()).price(new BigDecimal("250000")).build();
        booking.setTickets(List.of(ticket));
        pending = CancelRequest.builder().id(20L).booking(booking).status("PENDING")
                .reason("Thay đổi lịch trình").createdAt(LocalDateTime.now()).build();
    }

    private void submission() {
        when(currentUser.getCurrentCustomer()).thenReturn(customer);
        when(bookings.findOwnedByIdForUpdate(100L, customer.getId())).thenReturn(Optional.of(booking));
    }

    private void resolution() {
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        when(requests.findByIdForUpdate(20L)).thenReturn(Optional.of(pending));
    }

    private void approval() {
        resolution();
        when(bookings.findByIdForUpdate(100L)).thenReturn(Optional.of(booking));
    }

    @Test
    void customerSubmissionKeepsBookingAndSeatsValidUntilStaffApproves() {
        submission();
        when(tickets.findByBookingIdForUpdate(100L)).thenReturn(List.of(ticket));
        when(requests.save(any())).thenAnswer(invocation -> {
            CancelRequest request = invocation.getArgument(0);
            request.setId(20L);
            return request;
        });

        var result = service.submitCancelRequest(100L, "  Thay đổi lịch trình  ");

        assertEquals("PENDING", result.status());
        assertEquals("Thay đổi lịch trình", result.reason());
        assertNull(result.rejectReason());
        assertEquals("PAID", booking.getStatus());
        assertEquals("ISSUED", ticket.getStatus());
        assertEquals(List.of("A01"), result.booking().seats());
        verify(bookings, never()).save(any());
        verify(tickets, never()).saveAll(any());
    }

    @Test
    void customerCannotSubmitSomeoneElsesBooking() {
        when(currentUser.getCurrentCustomer()).thenReturn(customer);
        var exception = assertThrows(CustomException.class, () -> service.submitCancelRequest(100L, "Lý do"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(bookings).findOwnedByIdForUpdate(100L, customer.getId());
        verifyNoInteractions(requests, tickets);
    }

    @Test
    void retryReturnsSamePendingRequestWithoutCreatingDuplicates() {
        submission();
        when(requests.findByBooking_Id(100L)).thenReturn(Optional.of(pending));
        assertEquals(20L, service.submitCancelRequest(100L, "Gửi lại").id());
        verify(requests, never()).save(any());
        verifyNoInteractions(tickets);
    }

    @Test
    void alreadyResolvedRequestDoesNotCauseAUniqueConstraintFailure() {
        submission();
        pending.setStatus("REJECTED");
        when(requests.findByBooking_Id(100L)).thenReturn(Optional.of(pending));
        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.submitCancelRequest(100L, "Gửi lại")).getStatus());
        verify(requests, never()).save(any());
    }

    @Test
    void unpaidBookingCannotEnterPaidCancellationFlow() {
        submission();
        booking.setStatus("PENDING_PAYMENT");
        assertThrows(CustomException.class, () -> service.submitCancelRequest(100L, "Lý do"));
        verify(requests, never()).save(any());
    }

    @Test
    void missingOrBlankReasonIsRejected() {
        for (String reason : new String[]{null, "  ", "x".repeat(1001)}) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(CustomException.class,
                    () -> service.submitCancelRequest(100L, reason)).getStatus());
        }
        verifyNoInteractions(bookings, requests, tickets);
    }

    @Test
    void staffReceivesActualPendingRequestsIncludingLegacyReasons() {
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        pending.setReason(null);
        pending.setRejectReason("Lý do cũ của khách");
        when(requests.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(pending));

        var result = service.getRequests("PENDING");

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
        assertEquals("Lý do cũ của khách", result.get(0).reason());
        assertEquals("customer@example.com", result.get(0).booking().customer().email());
    }

    @Test
    void allStatusesAndInvalidFilterAreHandledExplicitly() {
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        when(requests.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pending));
        assertEquals(1, service.getRequests("all").size());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(CustomException.class,
                () -> service.getRequests("NOT_A_STATUS")).getStatus());
    }

    @Test
    void approvalCancelsBookingAndEveryTicketAndRecordsActualStaff() {
        approval();
        var second = Ticket.builder().id(1001L).booking(booking).status("RESERVED").build();
        when(tickets.findByBookingIdForUpdate(100L)).thenReturn(List.of(ticket, second));
        when(requests.save(pending)).thenReturn(pending);

        var result = service.resolveCancelRequest(20L, true, null);

        assertEquals("APPROVED", result.status());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("CANCELLED", ticket.getStatus());
        assertEquals("CANCELLED", second.getStatus());
        assertSame(staff, pending.getStaff());
        assertEquals(staff.getId(), result.staff().id());
        assertNotNull(result.resolvedAt());
        verify(tickets).saveAll(List.of(ticket, second));
        verify(bookings).save(booking);
    }

    @Test
    void rejectionLeavesBookingAndSeatsUnchangedAndPreservesLegacyReason() {
        resolution();
        pending.setReason(null);
        pending.setRejectReason("Khách bận việc");
        when(requests.save(pending)).thenReturn(pending);

        var result = service.resolveCancelRequest(20L, false, "  Không đủ điều kiện  ");

        assertEquals("REJECTED", result.status());
        assertEquals("Khách bận việc", result.reason());
        assertEquals("Không đủ điều kiện", result.rejectReason());
        assertEquals("PAID", booking.getStatus());
        assertEquals("ISSUED", ticket.getStatus());
        verifyNoInteractions(bookings, tickets);
    }

    @Test
    void rejectionRequiresNonBlankReason() {
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(20L, false, "  "));
        verifyNoInteractions(requests, bookings, tickets);
    }

    @Test
    void secondStaffCannotResolveAnAlreadyProcessedRequest() {
        resolution();
        pending.setStatus("APPROVED");
        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(20L, false, "Quyết định khác")).getStatus());
        verify(requests, never()).save(any());
        verifyNoInteractions(bookings, tickets);
    }

    @Test
    void approvalRechecksTicketUsageAndNeverPartiallyCancelsOnFailure() {
        approval();
        var used = Ticket.builder().id(1001L).status("USED").usedAt(LocalDateTime.now()).build();
        when(tickets.findByBookingIdForUpdate(100L)).thenReturn(List.of(ticket, used));

        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(20L, true, null)).getStatus());
        assertEquals("ISSUED", ticket.getStatus());
        assertEquals("PAID", booking.getStatus());
        assertEquals("PENDING", pending.getStatus());
        verify(tickets, never()).saveAll(any());
        verify(requests, never()).save(any());
    }

    @Test
    void approvalRechecksBookingStateChangedAfterCustomerSubmission() {
        approval();
        booking.setStatus("CANCELLED");
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(20L, true, null));
        assertEquals("PENDING", pending.getStatus());
        verifyNoInteractions(tickets);
    }

    @Test
    void departurePassedWhileWaitingCannotBeApproved() {
        approval();
        booking.getTrip().setDepartureTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(1));
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(20L, true, null));
        assertEquals("PENDING", pending.getStatus());
        verify(requests, never()).save(any());
    }

    @Test
    void missingRequestReturnsNotFound() {
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(999L, true, null)).getStatus());
    }

    @Test
    void customerRequestAppearsInStaffQueueAndApprovalReleasesItsTickets() {
        AtomicReference<CancelRequest> stored = new AtomicReference<>();
        submission();
        when(currentUser.getCurrentStaff()).thenReturn(staff);
        when(tickets.findByBookingIdForUpdate(100L)).thenReturn(List.of(ticket));
        when(bookings.findByIdForUpdate(100L)).thenReturn(Optional.of(booking));
        when(requests.save(any())).thenAnswer(invocation -> {
            CancelRequest request = invocation.getArgument(0);
            request.setId(20L);
            stored.set(request);
            return request;
        });
        when(requests.findByStatusOrderByCreatedAtAsc("PENDING")).thenAnswer(invocation ->
                stored.get() != null && "PENDING".equals(stored.get().getStatus())
                        ? List.of(stored.get()) : List.of());
        when(requests.findByIdForUpdate(20L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));

        assertEquals("PENDING", service.submitCancelRequest(100L, "Khách có việc bận").status());
        assertEquals("ISSUED", ticket.getStatus());
        assertEquals(20L, service.getRequests("PENDING").get(0).id());

        assertEquals("APPROVED", service.resolveCancelRequest(20L, true, null).status());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("CANCELLED", ticket.getStatus());
        assertTrue(service.getRequests("PENDING").isEmpty());
    }
}
