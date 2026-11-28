package vn.edu.iuh.nhom01_www.cancelrequest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CancelRequestServiceImpl implements CancelRequestService {

    private final CancelRequestRepository cancelRequestRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    private static final Set<String> CANCELLABLE_TICKET_STATUSES = Set.of("ISSUED", "RESERVED", "UNUSED");

    @Override
    @Transactional
    public CancelRequestResponse submitCancelRequest(Long bookingId, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 1000) {
            throw new CustomException("Vui lòng nhập lý do hủy vé (tối đa 1000 ký tự).", HttpStatus.BAD_REQUEST);
        }
        Customer customer = currentUserService.getCurrentCustomer();
        // Lock the booking before checking for duplicates, including two simultaneous submissions.
        Booking booking = bookingRepository.findOwnedByIdForUpdate(bookingId, customer.getId())
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé thuộc tài khoản của bạn.", HttpStatus.NOT_FOUND));

        var existing = cancelRequestRepository.findByBooking_Id(bookingId);
        if (existing.isPresent()) {
            if ("PENDING".equals(existing.get().getStatus())) {
                return CancelRequestResponse.from(existing.get());
            }
            throw new CustomException("Đơn đặt vé này đã có yêu cầu hủy được xử lý.", HttpStatus.CONFLICT);
        }
        validateBooking(booking);
        validateTickets(ticketRepository.findByBookingIdForUpdate(bookingId));

        CancelRequest cancelRequest = CancelRequest.builder()
                .booking(booking)
                .status("PENDING")
                .reason(reason.trim())
                .build();

        // Keep PAID / ISSUED and the occupied seats unchanged while waiting for staff.
        return CancelRequestResponse.from(cancelRequestRepository.save(cancelRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CancelRequestResponse> getRequests(String status) {
        currentUserService.getCurrentStaff();
        String filter = status == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING", "APPROVED", "REJECTED", "ALL").contains(filter)) {
            throw new CustomException("Trạng thái yêu cầu hủy không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        List<CancelRequest> requests = "ALL".equals(filter)
                ? cancelRequestRepository.findAllByOrderByCreatedAtDesc()
                : cancelRequestRepository.findByStatusOrderByCreatedAtAsc(filter);
        return requests.stream().map(CancelRequestResponse::from).toList();
    }

    @Override
    @Transactional
    public CancelRequestResponse resolveCancelRequest(Long cancelRequestId, boolean isApproved, String rejectReason) {
        Staff staff = currentUserService.getCurrentStaff();
        if (!isApproved && (rejectReason == null || rejectReason.isBlank() || rejectReason.length() > 1000)) {
            throw new CustomException("Vui lòng nhập lý do từ chối (tối đa 1000 ký tự).", HttpStatus.BAD_REQUEST);
        }
        CancelRequest request = cancelRequestRepository.findByIdForUpdate(cancelRequestId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu hủy vé.", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(request.getStatus())) {
            throw new CustomException("Yêu cầu này đã được nhân viên xử lý. Vui lòng tải lại danh sách.", HttpStatus.CONFLICT);
        }

        if (isApproved) {
            Booking booking = bookingRepository.findByIdForUpdate(request.getBooking().getId())
                    .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé.", HttpStatus.NOT_FOUND));
            validateBooking(booking);
            List<Ticket> tickets = ticketRepository.findByBookingIdForUpdate(booking.getId());
            validateTickets(tickets);
            tickets.forEach(Ticket::cancel);
            ticketRepository.saveAll(tickets);
            booking.cancelBooking();
            bookingRepository.save(booking);
            request.approve(staff);
        } else {
            // Preserve original customer reasons on requests submitted before the reason column existed.
            if (request.getReason() == null || request.getReason().isBlank()) {
                request.setReason(request.getRejectReason());
            }
            request.reject(staff, rejectReason.trim());
        }

        return CancelRequestResponse.from(cancelRequestRepository.save(request));
    }

    private void validateBooking(Booking booking) {
        if (!"PAID".equals(booking.getStatus())) {
            throw new CustomException("Chỉ có thể hủy đơn đặt vé đã thanh toán và còn hiệu lực.", HttpStatus.CONFLICT);
        }
        if (booking.getTrip() == null || booking.getTrip().getDepartureTime() == null
                || !booking.getTrip().getDepartureTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                || "COMPLETED".equalsIgnoreCase(booking.getTrip().getStatus())) {
            throw new CustomException("Chuyến xe đã khởi hành hoặc không còn lịch trình hợp lệ để hủy vé.", HttpStatus.CONFLICT);
        }
    }

    private void validateTickets(List<Ticket> tickets) {
        if (tickets.isEmpty() || tickets.stream().anyMatch(ticket ->
                ticket.getUsedAt() != null || ticket.getStatus() == null
                        || !CANCELLABLE_TICKET_STATUSES.contains(ticket.getStatus()))) {
            throw new CustomException("Đơn có vé đã sử dụng, đã hủy/đổi hoặc không còn hợp lệ để hủy toàn bộ.", HttpStatus.CONFLICT);
        }
    }
}
