package vn.edu.iuh.nhom01_www.cancelrequest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.user.service.AdminManagementService;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelRequestServiceImpl implements CancelRequestService {

    private final CancelRequestRepository cancelRequestRepository;
    private final BookingRepository bookingRepository;
    private final StaffRepository staffRepository;
    private final CurrentUserService currentUserService;
    private final vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CancelRequestResponse> search(
            String status, Long bookingId, int page, int size) {
        return searchForCustomer(status, bookingId, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CancelRequestResponse> mine(String status, int page, int size) {
        return searchForCustomer(status, null, currentUserService.getCurrentCustomer().getId(), page, size);
    }

    private PageResponse<CancelRequestResponse> searchForCustomer(
            String status, Long bookingId, UUID customerId, int page, int size) {
        if (status != null && !Set.of("PENDING", "APPROVED", "REJECTED").contains(status)) {
            throw new CustomException("Invalid cancel request status", HttpStatus.BAD_REQUEST);
        }
        Specification<CancelRequest> filter = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (status != null) predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            if (bookingId != null) predicate = cb.and(predicate, cb.equal(root.get("booking").get("id"), bookingId));
            if (customerId != null) predicate = cb.and(predicate, cb.equal(root.get("booking").get("customer").get("id"), customerId));
            return predicate;
        };
        var result = cancelRequestRepository.findAll(filter,
                AdminManagementService.pageable(page, size));
        return PageResponse.from(result.map(CancelRequestResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public CancelRequestResponse detail(Long id) {
        return CancelRequestResponse.from(cancelRequestRepository.findById(id)
                .orElseThrow(() -> new CustomException("Cancel request not found", HttpStatus.NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    public CancelRequestResponse myDetail(Long id) {
        UUID ownerId = currentUserService.getCurrentCustomer().getId();
        return CancelRequestResponse.from(cancelRequestRepository.findByIdAndBooking_Customer_Id(id, ownerId)
                .orElseThrow(() -> new CustomException("Cancel request not found", HttpStatus.NOT_FOUND)));
    }

    @Override
    @Transactional
    public CancelRequest submitCancelRequest(Long bookingId, String reason) {
        var customer = currentUserService.getCurrentCustomer();
        if (reason == null || reason.isBlank() || reason.length() > 2000) {
            throw new CustomException("A reason of at most 2000 characters is required", HttpStatus.BAD_REQUEST);
        }
        Booking booking = bookingRepository.findForUpdateById(bookingId)
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new CustomException("You do not own this booking", HttpStatus.FORBIDDEN);
        }
        ensureCancellable(booking);
        if (cancelRequestRepository.existsByBooking_Id(bookingId)) {
            throw new CustomException("A cancellation request already exists for this booking", HttpStatus.CONFLICT);
        }

        CancelRequest cancelRequest = CancelRequest.builder()
                .booking(booking)
                .status("PENDING")
                .reason(reason.trim())
                .build();

        return cancelRequestRepository.save(cancelRequest);
    }

    @Override
    @Transactional
    public CancelRequest resolveCancelRequest(Long cancelRequestId, UUID staffId, boolean isApproved, String rejectReason) {
        CancelRequest request = cancelRequestRepository.findForUpdateById(cancelRequestId)
                .orElseThrow(() -> new CustomException("Cancel Request not found", HttpStatus.NOT_FOUND));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new CustomException("Staff account is not active", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new CustomException("Cancel Request is already resolved", HttpStatus.CONFLICT);
        }

        Booking booking = bookingRepository.findForUpdateById(request.getBooking().getId())
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (isApproved) {
            // Lock ticket rows too: the atomic scanner update must not race approval.
            var tickets = ticketRepository.findForUpdateByBookingId(booking.getId());
            ensureCancellable(booking, tickets);
            if (request.getReason() == null) request.setReason(request.getRejectReason());
            request.setRejectReason(null);
            request.approve(staff);
            booking.cancelBooking();
            if (tickets != null) {
                for (Ticket ticket : tickets) {
                    ticket.cancel();
                }
            }
        } else {
            if (rejectReason == null || rejectReason.isBlank() || rejectReason.length() > 2000) {
                throw new CustomException("A rejection reason of at most 2000 characters is required", HttpStatus.BAD_REQUEST);
            }
            if (request.getReason() == null) request.setReason(request.getRejectReason());
            request.reject(staff, rejectReason.trim());
        }

        return cancelRequestRepository.save(request);
    }

    private void ensureCancellable(Booking booking) {
        ensureCancellable(booking, booking.getTickets());
    }

    private void ensureCancellable(Booking booking, java.util.List<Ticket> tickets) {
        if (!"PAID".equals(booking.getStatus())) {
            throw new CustomException("Only PAID bookings can be cancelled", HttpStatus.CONFLICT);
        }
        if (booking.getTrip() == null || booking.getTrip().getDepartureTime() == null
                || !booking.getTrip().getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new CustomException("Cannot cancel a booking after departure", HttpStatus.CONFLICT);
        }
        if (tickets == null || tickets.isEmpty()
                || tickets.stream().anyMatch(t -> !"ISSUED".equals(t.getStatus()))) {
            throw new CustomException("All tickets must be ISSUED to cancel the whole booking", HttpStatus.CONFLICT);
        }
    }
}
