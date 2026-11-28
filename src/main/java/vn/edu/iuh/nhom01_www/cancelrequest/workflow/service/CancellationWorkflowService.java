package vn.edu.iuh.nhom01_www.cancelrequest.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.model.CancelWorkflowDetails;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository.CancelWorkflowDetailsRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository.CancellationQueries;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CancellationWorkflowService {
    public static final String FINANCIAL_NOTICE = "Duyệt hủy chỉ hủy booking và vé; chức năng này không hoàn tiền hoặc sửa giao dịch thanh toán.";
    private final CancellationQueries queries;
    private final CancelRequestRepository requests;
    private final CancelWorkflowDetailsRepository details;
    private final BookingRepository bookings;
    private final TicketRepository tickets;
    private final PaymentRepository payments;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Autowired
    public CancellationWorkflowService(CancellationQueries queries, CancelRequestRepository requests,
            CancelWorkflowDetailsRepository details, BookingRepository bookings, TicketRepository tickets,
            PaymentRepository payments, CurrentUserService currentUser) {
        this(queries, requests, details, bookings, tickets, payments, currentUser, Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public CancellationWorkflowService(CancellationQueries queries, CancelRequestRepository requests,
            CancelWorkflowDetailsRepository details, BookingRepository bookings, TicketRepository tickets,
            PaymentRepository payments, CurrentUserService currentUser, Clock clock) {
        this.queries = queries; this.requests = requests; this.details = details; this.bookings = bookings;
        this.tickets = tickets; this.payments = payments; this.currentUser = currentUser;
        this.clock = clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public PageResult<RequestRow> staffList(String keyword, String status, int page, int size) {
        activeStaff();
        return list(keyword, status, null, page, size);
    }

    public PageResult<RequestRow> myRequests(String status, int page, int size) {
        return list("", status, activeCustomer().getId(), page, size);
    }

    private PageResult<RequestRow> list(String keyword, String status, UUID owner, int page, int size) {
        if (page < 0 || page > 1000000 || size < 1 || size > 100 || (keyword != null && keyword.length() > 200)) {
            throw badRequest("Tham số không hợp lệ: trang từ 0, số dòng từ 1 đến 100, từ khóa tối đa 200 ký tự.");
        }
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalized != null && !Set.of("PENDING", "APPROVED", "REJECTED").contains(normalized)) throw badRequest("Trạng thái yêu cầu không hợp lệ.");
        var result = queries.list(keyword, normalized, owner, PageRequest.of(page, size));
        Map<Long, CancelWorkflowDetails> metadata = details.findAllById(result.getContent().stream().map(CancelRequest::getId).toList())
                .stream().collect(Collectors.toMap(CancelWorkflowDetails::getRequestId, Function.identity()));
        return new PageResult<>(result.getContent().stream().map(request -> row(request, metadata.get(request.getId()))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public RequestDetails staffDetails(Long id) {
        activeStaff();
        return describe(request(id, null));
    }

    public RequestDetails myDetails(Long id) {
        return describe(request(id, activeCustomer().getId()));
    }

    public BookingInfo lookup(Long bookingId) {
        activeStaff();
        Booking booking = queries.booking(bookingId, null, false).orElseThrow(this::bookingNotFound);
        return bookingInfo(booking, null);
    }

    @Transactional
    public RequestDetails submitByCustomer(SubmitRequest input) {
        Customer actor = activeCustomer();
        return submit(input, actor.getId(), actor.getEmail(), "CUSTOMER");
    }

    @Transactional
    public RequestDetails submitByStaff(SubmitRequest input) {
        Staff actor = activeStaff();
        return submit(input, null, actor.getEmail(), "STAFF");
    }

    @Transactional
    public RequestDetails submitFromProfile(Long bookingId) {
        Customer actor = activeCustomer();
        // The old profile sends only a booking ID. Preserve the missing reason honestly instead of inventing one.
        return submit(new SubmitRequest(bookingId, null), actor.getId(), actor.getEmail(), "PROFILE");
    }

    private RequestDetails submit(SubmitRequest input, UUID owner, String email, String source) {
        String reason = "PROFILE".equals(source) ? null : requiredReason(input.reason());
        Booking booking = queries.booking(input.bookingId(), owner, true).orElseThrow(this::bookingNotFound);
        if (queries.requestIdForBooking(booking.getId()).isPresent()) {
            throw conflict("Booking đã có yêu cầu hủy. Mỗi booking chỉ có một yêu cầu theo cấu trúc hiện tại.");
        }
        List<Ticket> lockedTickets = queries.tickets(booking.getId(), true);
        Payment payment = payments.findByBookingId(booking.getId()).orElse(null);
        requireEligible(booking, lockedTickets, payment);
        CancelRequest request = new CancelRequest();
        request.setBooking(booking); request.setStatus("PENDING");
        requests.saveAndFlush(request);
        CancelWorkflowDetails metadata = details.saveAndFlush(new CancelWorkflowDetails(request, reason, email, source, now()));
        // Pending cancellation is not a new Booking state: tickets remain valid until approval.
        return describe(request, metadata, lockedTickets, payment);
    }

    @Transactional
    public RequestDetails approve(Long id, ApproveRequest input) {
        Staff actor = activeStaff();
        if (!Boolean.TRUE.equals(input.confirm())) throw badRequest("Cần xác nhận hủy toàn bộ booking.");
        LockedRequest locked = pending(id, input.version());
        List<Ticket> lockedTickets = queries.tickets(locked.booking().getId(), true);
        Payment payment = payments.findByBookingId(locked.booking().getId()).orElse(null);
        requireEligible(locked.booking(), lockedTickets, payment);
        lockedTickets.forEach(Ticket::cancel);
        locked.booking().cancelBooking();
        tickets.saveAllAndFlush(lockedTickets);
        bookings.saveAndFlush(locked.booking());
        locked.request().approve(actor);
        locked.request().setResolvedAt(now());
        locked.request().setRejectReason(null);
        requests.saveAndFlush(locked.request());
        locked.metadata().recordDecision(optionalNote(input.note()), now());
        details.saveAndFlush(locked.metadata());
        return describe(locked.request(), locked.metadata(), lockedTickets, payment);
    }

    @Transactional
    public RequestDetails reject(Long id, RejectRequest input) {
        Staff actor = activeStaff();
        String reason = requiredReason(input.reason());
        if (reason.length() > 255) throw badRequest("Lý do từ chối tối đa 255 ký tự để phù hợp dữ liệu hiện có.");
        LockedRequest locked = pending(id, input.version());
        locked.request().reject(actor, reason);
        locked.request().setResolvedAt(now());
        requests.saveAndFlush(locked.request());
        locked.metadata().recordDecision(reason, now());
        details.saveAndFlush(locked.metadata());
        // Rejection remains possible after departure/check-in; it never changes the booking or tickets.
        return describe(locked.request());
    }

    private LockedRequest pending(Long id, Long expectedVersion) {
        Long bookingId = queries.bookingIdForRequest(id).orElseThrow(this::requestNotFound);
        // Stable lock order shared with submission: booking -> request -> tickets.
        Booking booking = queries.booking(bookingId, null, true).orElseThrow(this::bookingNotFound);
        CancelRequest request = queries.lockRequest(id);
        if (request == null) throw requestNotFound();
        if (!"PENDING".equals(status(request.getStatus()))) throw conflict("Yêu cầu đã được xử lý; không thể duyệt hoặc từ chối lần nữa.");
        CancelWorkflowDetails metadata = details.findById(id).orElse(null);
        Long actualVersion = metadata == null ? 0L : metadata.getVersion();
        if (expectedVersion == null || !Objects.equals(expectedVersion, actualVersion)) {
            throw conflict("Yêu cầu đã thay đổi. Vui lòng đóng hộp thoại và tải lại danh sách.");
        }
        if (metadata == null) {
            metadata = details.saveAndFlush(new CancelWorkflowDetails(request, request.getRejectReason(), null, "LEGACY",
                    request.getCreatedAt() == null ? now() : request.getCreatedAt()));
        }
        return new LockedRequest(request, booking, metadata);
    }

    private RequestDetails describe(CancelRequest request) {
        return describe(request, details.findById(request.getId()).orElse(null),
                queries.tickets(request.getBooking().getId(), false), payments.findByBookingId(request.getBooking().getId()).orElse(null));
    }

    private RequestDetails describe(CancelRequest request, CancelWorkflowDetails metadata, List<Ticket> rows, Payment payment) {
        BookingInfo info = bookingInfo(request.getBooking(), request.getId(), rows, payment);
        return new RequestDetails(row(request, metadata), info, metadata == null ? null : metadata.getDecisionNote(),
                "PENDING".equals(status(request.getStatus())) && info.eligible(), FINANCIAL_NOTICE);
    }

    private BookingInfo bookingInfo(Booking booking, Long ignoredRequestId) {
        return bookingInfo(booking, ignoredRequestId, queries.tickets(booking.getId(), false), payments.findByBookingId(booking.getId()).orElse(null));
    }

    private BookingInfo bookingInfo(Booking booking, Long ignoredRequestId, List<Ticket> rows, Payment payment) {
        Long existing = queries.requestIdForBooking(booking.getId()).orElse(null);
        String blocked = ineligible(booking, rows, payment);
        if (blocked == null && existing != null && !existing.equals(ignoredRequestId)) blocked = "Booking đã có yêu cầu hủy #" + existing + ".";
        var customer = booking.getCustomer();
        return new BookingInfo(booking.getId(), "BKG-" + booking.getId(), customer.getFullName(), customer.getEmail(), customer.getPhoneNumber(),
                route(booking), booking.getTrip() == null ? null : booking.getTrip().getDepartureTime(), status(booking.getStatus()), booking.getTotalPrice(),
                payment == null ? null : payment.getAmount(), payment == null ? null : status(payment.getStatus()),
                rows.stream().map(ticket -> new TicketInfo(ticket.getId(), ticket.getTicketCode(),
                        ticket.getSeat() == null ? null : ticket.getSeat().getSeatName(), status(ticket.getStatus()), ticket.getUsedAt())).toList(),
                existing, blocked == null, blocked);
    }

    private RequestRow row(CancelRequest request, CancelWorkflowDetails metadata) {
        Booking booking = request.getBooking();
        var customer = booking.getCustomer();
        String state = status(request.getStatus());
        String reason = metadata != null ? metadata.getRequestReason() : "PENDING".equals(state) ? request.getRejectReason() : null;
        return new RequestRow(request.getId(), booking.getId(), "BKG-" + booking.getId(), customer.getFullName(), customer.getEmail(),
                customer.getPhoneNumber(), route(booking), booking.getTrip() == null ? null : booking.getTrip().getDepartureTime(), state, status(booking.getStatus()), reason,
                "REJECTED".equals(state) ? request.getRejectReason() : null,
                metadata == null ? request.getCreatedAt() : metadata.getSubmittedAt(), request.getResolvedAt(),
                request.getStaff() == null ? null : request.getStaff().getFullName(), metadata == null ? "LEGACY" : metadata.getSource(),
                metadata == null ? 0L : metadata.getVersion());
    }

    private String ineligible(Booking booking, List<Ticket> rows, Payment payment) {
        if (!"PAID".equals(status(booking.getStatus()))) return "Chỉ tiếp nhận và duyệt hủy booking đã thanh toán (PAID).";
        if (payment == null || !"SUCCESS".equals(status(payment.getStatus()))) return "Chưa có giao dịch thanh toán thành công để đối chiếu.";
        if (booking.getTrip() == null || booking.getTrip().getDepartureTime() == null) return "Không xác định được giờ khởi hành.";
        if (Set.of("RUNNING", "COMPLETED").contains(status(booking.getTrip().getStatus())) || !now().isBefore(booking.getTrip().getDepartureTime())) return "Chuyến xe đã khởi hành hoặc hoàn thành; không áp dụng quy trình hủy thông thường.";
        if (rows.isEmpty()) return "Booking không có vé để hủy.";
        boolean active = false;
        for (Ticket ticket : rows) {
            String state = status(ticket.getStatus());
            if (ticket.getUsedAt() != null || "USED".equals(state)) return "Booking có vé đã sử dụng; không thể hủy toàn bộ đơn.";
            if (!Set.of("ISSUED", "UNUSED", "RESERVED", "CANCELLED").contains(state)) return "Booking có vé không đủ điều kiện hủy (" + state + ").";
            active |= !"CANCELLED".equals(state);
        }
        return active ? null : "Toàn bộ vé trong booking đã bị hủy.";
    }

    private void requireEligible(Booking booking, List<Ticket> rows, Payment payment) {
        String blocked = ineligible(booking, rows, payment);
        if (blocked != null) throw conflict(blocked);
    }

    private CancelRequest request(Long id, UUID owner) {
        return queries.request(id, owner).orElseThrow(this::requestNotFound);
    }

    private Staff activeStaff() {
        Staff actor = currentUser.getCurrentStaff();
        if (!"ACTIVE".equals(status(actor.getStatus()))) throw new CustomException("Tài khoản nhân viên không hoạt động.", HttpStatus.FORBIDDEN);
        return actor;
    }

    private Customer activeCustomer() {
        Customer actor = currentUser.getCurrentCustomer();
        if (!"ACTIVE".equals(status(actor.getStatus()))) throw new CustomException("Tài khoản khách hàng không hoạt động.", HttpStatus.FORBIDDEN);
        return actor;
    }

    private String requiredReason(String value) {
        if (value == null || value.isBlank() || value.length() > 1000) throw badRequest("Lý do không được để trống và không vượt quá 1000 ký tự.");
        return value.trim();
    }

    private String optionalNote(String value) {
        if (value != null && value.length() > 1000) throw badRequest("Ghi chú không được vượt quá 1000 ký tự.");
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String status(String value) { return value == null ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private String route(Booking booking) {
        return booking.getTrip() == null || booking.getTrip().getRoute() == null ? "Chưa xác định tuyến"
                : booking.getTrip().getRoute().getDepartureLocation() + " → " + booking.getTrip().getRoute().getArrivalLocation();
    }
    private CustomException badRequest(String message) { return new CustomException(message, HttpStatus.BAD_REQUEST); }
    private CustomException conflict(String message) { return new CustomException(message, HttpStatus.CONFLICT); }
    private CustomException bookingNotFound() { return new CustomException("Không tìm thấy booking hoặc bạn không có quyền truy cập.", HttpStatus.NOT_FOUND); }
    private CustomException requestNotFound() { return new CustomException("Không tìm thấy yêu cầu hủy hoặc bạn không có quyền truy cập.", HttpStatus.NOT_FOUND); }
    private record LockedRequest(CancelRequest request, Booking booking, CancelWorkflowDetails metadata) { }
}
