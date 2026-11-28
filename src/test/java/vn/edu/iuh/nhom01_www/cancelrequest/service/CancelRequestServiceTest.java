package vn.edu.iuh.nhom01_www.cancelrequest.service;

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
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelRequestServiceTest {
    @Mock CancelRequestRepository cancelRequestRepository;
    @Mock BookingRepository bookingRepository;
    @Mock StaffRepository staffRepository;
    @Mock CurrentUserService currentUserService;
    @Mock vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository ticketRepository;
    @InjectMocks CancelRequestServiceImpl service;
    Customer customer;
    Staff staff;
    Booking booking;
    CancelRequest request;

    @BeforeEach void setup() {
        customer = Customer.builder().id(UUID.randomUUID()).fullName("Customer").build();
        staff = Staff.builder().id(UUID.randomUUID()).status("ACTIVE").build();
        booking = Booking.builder().id(1L).customer(customer).status("PAID")
                .trip(Trip.builder().id(2L).departureTime(LocalDateTime.now().plusDays(1)).build())
                .tickets(List.of(Ticket.builder().id(3L).status("ISSUED").build())).build();
        request = CancelRequest.builder().id(4L).booking(booking).status("PENDING").reason("Change of plans").build();
    }

    @Test void ownerCanSubmitAndReasonIsNotStoredAsRejection() {
        prepareSubmit();
        when(cancelRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.submitCancelRequest(1L, " Change of plans ");
        assertEquals("PENDING", result.getStatus());
        assertEquals("Change of plans", result.getReason());
        assertNull(result.getRejectReason());
    }

    @Test void refusesOtherCustomersBooking() {
        prepareSubmit();
        booking.setCustomer(Customer.builder().id(UUID.randomUUID()).build());
        var error = assertThrows(CustomException.class, () -> service.submitCancelRequest(1L, "Reason"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(cancelRequestRepository, never()).save(any());
    }

    @Test void refusesDuplicateSubmission() {
        prepareSubmit();
        when(cancelRequestRepository.existsByBooking_Id(1L)).thenReturn(true);
        assertThrows(CustomException.class, () -> service.submitCancelRequest(1L, "Reason"));
    }

    @Test void refusesUnpaidBooking() {
        prepareSubmit();
        booking.setStatus("PENDING_PAYMENT");
        assertThrows(CustomException.class, () -> service.submitCancelRequest(1L, "Reason"));
    }

    @Test void approvalCancelsBookingAndTicketsAndRecordsActor() {
        prepareResolve();
        when(ticketRepository.findForUpdateByBookingId(1L)).thenReturn(booking.getTickets());
        when(cancelRequestRepository.save(request)).thenReturn(request);
        service.resolveCancelRequest(4L, staff.getId(), true, null);
        assertEquals("APPROVED", request.getStatus());
        assertEquals(staff, request.getStaff());
        assertNotNull(request.getResolvedAt());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("CANCELLED", booking.getTickets().get(0).getStatus());
    }

    @Test void rejectionPreservesTicketsAndOriginalReason() {
        prepareResolve();
        when(cancelRequestRepository.save(request)).thenReturn(request);
        service.resolveCancelRequest(4L, staff.getId(), false, " Not eligible ");
        assertEquals("REJECTED", request.getStatus());
        assertEquals("Change of plans", request.getReason());
        assertEquals("Not eligible", request.getRejectReason());
        assertEquals("PAID", booking.getStatus());
        assertEquals("ISSUED", booking.getTickets().get(0).getStatus());
    }

    @Test void rejectionRequiresExplanation() {
        prepareResolve();
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(4L, staff.getId(), false, " "));
        assertEquals("PENDING", request.getStatus());
    }

    @Test void cannotProcessTwice() {
        when(cancelRequestRepository.findForUpdateById(4L)).thenReturn(Optional.of(request));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        request.setStatus("APPROVED");
        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(4L, staff.getId(), true, null)).getStatus());
        verify(bookingRepository, never()).findForUpdateById(any());
    }

    @Test void cannotApproveAfterDeparture() {
        prepareResolve();
        booking.getTrip().setDepartureTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(4L, staff.getId(), true, null));
    }

    @Test void cannotApproveCheckedInTicket() {
        prepareResolve();
        when(ticketRepository.findForUpdateByBookingId(1L)).thenReturn(booking.getTickets());
        booking.getTickets().get(0).setStatus("USED");
        assertThrows(CustomException.class, () -> service.resolveCancelRequest(4L, staff.getId(), true, null));
        assertEquals("PAID", booking.getStatus());
    }

    @Test void lockedStaffCannotResolve() {
        when(cancelRequestRepository.findForUpdateById(4L)).thenReturn(Optional.of(request));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        staff.setStatus("LOCKED");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(4L, staff.getId(), true, null)).getStatus());
    }

    @Test void invalidSearchStatusIsRejected() {
        assertThrows(CustomException.class, () -> service.search("ANY", null, 0, 20));
        verifyNoInteractions(cancelRequestRepository);
    }

    @Test void approvalReadsLockedTicketStateNotStaleBookingCollection() {
        prepareResolve();
        when(ticketRepository.findForUpdateByBookingId(1L))
                .thenReturn(List.of(Ticket.builder().id(3L).status("USED").build()));
        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(4L, staff.getId(), true, null)).getStatus());
        assertEquals("PENDING", request.getStatus());
        assertEquals("PAID", booking.getStatus());
        verify(cancelRequestRepository, never()).save(any());
    }

    @Test void secondStaffCannotResolveAlreadyRejectedRequest() {
        when(cancelRequestRepository.findForUpdateById(4L)).thenReturn(Optional.of(request));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        request.reject(staff, "Not eligible");
        assertEquals(HttpStatus.CONFLICT, assertThrows(CustomException.class,
                () -> service.resolveCancelRequest(4L, staff.getId(), true, null)).getStatus());
        verifyNoInteractions(bookingRepository, ticketRepository);
        assertEquals("REJECTED", request.getStatus());
    }

    @Test void customerDetailAlwaysUsesAuthenticatedOwner() {
        when(currentUserService.getCurrentCustomer()).thenReturn(customer);
        when(cancelRequestRepository.findByIdAndBooking_Customer_Id(4L, customer.getId())).thenReturn(Optional.of(request));
        assertEquals(4L, service.myDetail(4L).id());
        verify(cancelRequestRepository).findByIdAndBooking_Customer_Id(4L, customer.getId());
        verify(cancelRequestRepository, never()).findById(any());
    }

    @Test void otherCustomersDetailIsNotFound() {
        when(currentUserService.getCurrentCustomer()).thenReturn(customer);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(CustomException.class, () -> service.myDetail(4L)).getStatus());
    }

    @Test void mineRejectsInvalidStatusAndPagination() {
        when(currentUserService.getCurrentCustomer()).thenReturn(customer);
        assertThrows(CustomException.class, () -> service.mine("UNKNOWN", 0, 20));
        assertThrows(CustomException.class, () -> service.mine(null, -1, 20));
        assertThrows(CustomException.class, () -> service.mine(null, 0, 101));
        verifyNoInteractions(cancelRequestRepository);
    }

    @Test void emptyReasonCannotCreateRequest() {
        when(currentUserService.getCurrentCustomer()).thenReturn(customer);
        assertThrows(CustomException.class, () -> service.submitCancelRequest(1L, "   "));
        verifyNoInteractions(bookingRepository, cancelRequestRepository);
    }

    @Test void responseContainsTicketAndResolutionDetailsWithoutSecrets() throws Exception {
        booking.getTrip().setRoute(vn.edu.iuh.nhom01_www.route.model.Route.builder()
                .departureLocation("TP.HCM").arrivalLocation("Đà Lạt").build());
        booking.getTickets().get(0).setSeat(vn.edu.iuh.nhom01_www.seat.model.Seat.builder().seatName("A01").build());
        customer.setPassword("secret-customer-hash");
        staff.setPassword("secret-staff-hash");
        staff.setFullName("Nhân viên A");
        request.reject(staff, "Lý do từ chối");
        var response = vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse.from(request);
        assertEquals("A01", response.tickets().get(0).seatName());
        assertEquals("Đà Lạt", response.arrivalLocation());
        assertEquals("Nhân viên A", response.resolvedByName());
        assertFalse(response.canApprove());
        var json = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()).writeValueAsString(response);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-"));
    }

    private void prepareSubmit() {
        when(currentUserService.getCurrentCustomer()).thenReturn(customer);
        when(bookingRepository.findForUpdateById(1L)).thenReturn(Optional.of(booking));
    }

    private void prepareResolve() {
        when(cancelRequestRepository.findForUpdateById(4L)).thenReturn(Optional.of(request));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(bookingRepository.findForUpdateById(1L)).thenReturn(Optional.of(booking));
    }
}
