package vn.edu.iuh.nhom01_www.cancelrequest.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.model.CancelWorkflowDetails;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancellationWorkflowServiceTest {
    @Mock private CancellationQueries queries;
    @Mock private CancelRequestRepository requests;
    @Mock private CancelWorkflowDetailsRepository metadata;
    @Mock private BookingRepository bookings;
    @Mock private TicketRepository tickets;
    @Mock private PaymentRepository payments;
    @Mock private CurrentUserService currentUser;
    private CancellationWorkflowService service;
    private Booking booking;
    private List<Ticket> bookingTickets;
    private Payment payment;
    private Customer owner;
    private Staff actor;
    private CancelRequest request;
    private CancelWorkflowDetails details;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-27T03:00:00Z"), ZoneOffset.UTC);
        service = new CancellationWorkflowService(queries, requests, metadata, bookings, tickets, payments, currentUser, clock);
        owner = Customer.builder().id(UUID.randomUUID()).fullName("Khách hàng").email("owner@example.test").status("ACTIVE").build();
        actor = Staff.builder().id(UUID.randomUUID()).fullName("Nhân viên").email("staff@example.test").status("ACTIVE")
                .role(Role.builder().name("ROLE_STAFF").build()).build();
        Route route = Route.builder().departureLocation("TP.HCM").arrivalLocation("Đà Lạt").build();
        Trip trip = Trip.builder().id(3L).route(route).departureTime(LocalDateTime.of(2026, 11, 29, 20, 0)).status("PENDING").build();
        booking = Booking.builder().id(7L).customer(owner).trip(trip).status("PAID").totalPrice(new BigDecimal("500000")).build();
        bookingTickets = List.of(Ticket.builder().id(1L).booking(booking).ticketCode("T1").status("ISSUED").seat(Seat.builder().seatName("A01").build()).build(),
                Ticket.builder().id(2L).booking(booking).ticketCode("T2").status("ISSUED").seat(Seat.builder().seatName("A02").build()).build());
        payment = Payment.builder().id(8L).booking(booking).status("SUCCESS").amount(new BigDecimal("500000")).transactionId("PAY-ORIGINAL").build();
        request = CancelRequest.builder().id(42L).booking(booking).status("PENDING").createdAt(LocalDateTime.of(2026, 11, 27, 9, 0)).build();
        details = new CancelWorkflowDetails(request, "Khách đổi kế hoạch", owner.getEmail(), "CUSTOMER", request.getCreatedAt());
        ReflectionTestUtils.setField(details, "version", 0L);
    }

    @Test
    void submitsCustomerRequestWithOwnershipConstraintAndDoesNotCancelBooking() {
        when(currentUser.getCurrentCustomer()).thenReturn(owner);
        when(queries.booking(7L, owner.getId(), true)).thenReturn(Optional.of(booking));
        when(queries.tickets(7L, true)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        when(requests.saveAndFlush(any(CancelRequest.class))).thenAnswer(call -> { CancelRequest value = call.getArgument(0); value.setId(42L); return value; });
        when(metadata.saveAndFlush(any())).thenAnswer(call -> { CancelWorkflowDetails value = call.getArgument(0); ReflectionTestUtils.setField(value, "version", 0L); return value; });
        RequestDetails result = service.submitByCustomer(new SubmitRequest(7L, "  Khách đổi kế hoạch  "));
        assertThat(result.request().requestReason()).isEqualTo("Khách đổi kế hoạch");
        assertThat(result.request().status()).isEqualTo("PENDING");
        assertThat(result.booking().status()).isEqualTo("PAID");
        assertThat(bookingTickets).allMatch(ticket -> ticket.getStatus().equals("ISSUED"));
        verify(queries).booking(7L, owner.getId(), true);
        verifyNoInteractions(bookings, tickets);
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsForeignBookingWithoutSavingARequest() {
        when(currentUser.getCurrentCustomer()).thenReturn(owner);
        assertThatThrownBy(() -> service.submitByCustomer(new SubmitRequest(7L, "Lý do")))
                .isInstanceOfSatisfying(CustomException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(queries).booking(7L, owner.getId(), true);
        verifyNoInteractions(requests, metadata, payments);
    }

    @Test
    void rejectsDuplicateBeforeTicketOrPaymentProcessing() {
        when(currentUser.getCurrentCustomer()).thenReturn(owner);
        when(queries.booking(7L, owner.getId(), true)).thenReturn(Optional.of(booking));
        when(queries.requestIdForBooking(7L)).thenReturn(Optional.of(42L));
        assertThatThrownBy(() -> service.submitByCustomer(new SubmitRequest(7L, "Lý do"))).hasMessageContaining("đã có yêu cầu");
        verifyNoInteractions(requests, metadata, payments);
    }

    @Test
    void lookupRejectsUnpaidMissingPaymentAndUsedTickets() {
        staff();
        when(queries.booking(7L, null, false)).thenReturn(Optional.of(booking));
        when(queries.tickets(7L, false)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        booking.setStatus("PENDING_PAYMENT");
        assertThat(service.lookup(7L).eligible()).isFalse();
        booking.setStatus("PAID"); payment.setStatus("FAILED");
        assertThat(service.lookup(7L).ineligibleReason()).contains("thanh toán thành công");
        payment.setStatus("SUCCESS"); bookingTickets.get(0).use();
        assertThat(service.lookup(7L).ineligibleReason()).contains("đã sử dụng");
    }

    @Test
    void lookupRejectsDepartedRunningRescheduledAndFullyCancelledBookings() {
        staff();
        when(queries.booking(7L, null, false)).thenReturn(Optional.of(booking));
        when(queries.tickets(7L, false)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        booking.getTrip().setStatus("RUNNING");
        assertThat(service.lookup(7L).ineligibleReason()).contains("đã khởi hành");
        booking.getTrip().setStatus("PENDING"); bookingTickets.get(0).setStatus("RESCHEDULED");
        assertThat(service.lookup(7L).ineligibleReason()).contains("RESCHEDULED");
        bookingTickets.forEach(Ticket::cancel);
        assertThat(service.lookup(7L).ineligibleReason()).contains("Toàn bộ vé");
    }

    @Test
    void approvalCancelsWholeBookingAndPreservesPayment() {
        pending();
        when(queries.tickets(7L, true)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        RequestDetails result = service.approve(42L, new ApproveRequest(0L, true, "Đã liên hệ khách"));
        assertThat(result.request().status()).isEqualTo("APPROVED");
        assertThat(result.booking().status()).isEqualTo("CANCELLED");
        assertThat(result.booking().tickets()).allMatch(ticket -> ticket.status().equals("CANCELLED"));
        assertThat(result.request().processedBy()).isEqualTo(actor.getFullName());
        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        verify(payments, never()).save(any());
        verify(tickets).saveAllAndFlush(bookingTickets);
        verify(bookings).saveAndFlush(booking);
    }

    @Test
    void approvalRequiresExplicitConfirmation() {
        staff();
        assertThatThrownBy(() -> service.approve(42L, new ApproveRequest(0L, false, null))).hasMessageContaining("xác nhận");
        verifyNoInteractions(queries, requests, bookings, tickets, payments);
    }

    @Test
    void rejectsStaleVersionAndAlreadyResolvedRequests() {
        pending();
        assertThatThrownBy(() -> service.approve(42L, new ApproveRequest(99L, true, null))).hasMessageContaining("đã thay đổi");
        request.setStatus("APPROVED");
        assertThatThrownBy(() -> service.approve(42L, new ApproveRequest(0L, true, null))).hasMessageContaining("đã được xử lý");
        verifyNoInteractions(bookings, tickets, payments);
    }

    @Test
    void approvalRechecksEligibilityBeforeAnyWrites() {
        pending();
        when(queries.tickets(7L, true)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        bookingTickets.get(0).use();
        assertThatThrownBy(() -> service.approve(42L, new ApproveRequest(0L, true, null))).hasMessageContaining("đã sử dụng");
        assertThat(request.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getStatus()).isEqualTo("PAID");
        verifyNoInteractions(bookings, tickets);
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBlankDecisionReasonBeforeLoadingRequest() {
        staff();
        assertThatThrownBy(() -> service.reject(42L, new RejectRequest(0L, "  "))).hasMessageContaining("Lý do");
        assertThatThrownBy(() -> service.reject(42L, new RejectRequest(0L, "A".repeat(256)))).hasMessageContaining("255 ký tự");
        verifyNoInteractions(queries, requests, metadata);
    }

    @Test
    void rejectionPreservesOriginalRequestReasonAndDoesNotChangeBookingOrTickets() {
        pending();
        when(queries.tickets(7L, false)).thenReturn(bookingTickets);
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        RequestDetails result = service.reject(42L, new RejectRequest(0L, "Khách đã xác nhận giữ vé"));
        assertThat(result.request().status()).isEqualTo("REJECTED");
        assertThat(result.request().requestReason()).isEqualTo("Khách đổi kế hoạch");
        assertThat(result.request().rejectionReason()).isEqualTo("Khách đã xác nhận giữ vé");
        assertThat(booking.getStatus()).isEqualTo("PAID");
        assertThat(bookingTickets).allMatch(ticket -> ticket.getStatus().equals("ISSUED"));
        verifyNoInteractions(bookings, tickets);
        verify(payments, never()).save(any());
    }

    @Test
    void listsRequestsWithSupplementaryReasonsAndSupportsLegacyRows() {
        staff();
        when(queries.list("", "PENDING", null, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1));
        when(metadata.findAllById(List.of(42L))).thenReturn(List.of(details));
        assertThat(service.staffList("", "PENDING", 0, 10).content().get(0).requestReason()).isEqualTo("Khách đổi kế hoạch");
    }

    @Test
    void preventsCustomersReadingForeignRequestsAndScopesTheirHistory() {
        when(currentUser.getCurrentCustomer()).thenReturn(owner);
        assertThatThrownBy(() -> service.myDetails(42L)).hasMessageContaining("không có quyền");
        verify(queries).request(42L, owner.getId());
    }

    @Test
    void rejectsInvalidPaginationStatusAndKeyword() {
        staff();
        assertThatThrownBy(() -> service.staffList("", "UNKNOWN", 0, 10)).hasMessageContaining("Trạng thái");
        assertThatThrownBy(() -> service.staffList("", "", -1, 10)).hasMessageContaining("Tham số");
        assertThatThrownBy(() -> service.staffList("", "", 0, 101)).hasMessageContaining("Tham số");
        assertThatThrownBy(() -> service.staffList("A".repeat(201), "", 0, 10)).hasMessageContaining("Tham số");
        verifyNoInteractions(queries);
    }

    @Test
    void rejectsInactiveStaffAndCustomer() {
        staff(); actor.setStatus("LOCKED");
        assertThatThrownBy(() -> service.lookup(7L)).isInstanceOfSatisfying(CustomException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        when(currentUser.getCurrentCustomer()).thenReturn(owner); owner.setStatus("LOCKED");
        assertThatThrownBy(() -> service.myRequests("", 0, 10)).hasMessageContaining("không hoạt động");
        verifyNoInteractions(queries);
    }

    private void staff() { when(currentUser.getCurrentStaff()).thenReturn(actor); }
    private void pending() {
        staff();
        when(queries.bookingIdForRequest(42L)).thenReturn(Optional.of(7L));
        when(queries.booking(7L, null, true)).thenReturn(Optional.of(booking));
        when(queries.lockRequest(42L)).thenReturn(request);
        when(metadata.findById(42L)).thenReturn(Optional.of(details));
    }
}
