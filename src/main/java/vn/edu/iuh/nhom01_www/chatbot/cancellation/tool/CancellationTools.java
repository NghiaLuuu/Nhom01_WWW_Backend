package vn.edu.iuh.nhom01_www.chatbot.cancellation.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.aivector.VietnameseLocationNormalizer;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.*;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.service.CancellationPolicyService;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.service.CancellationPreviewManager;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationTools {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final CancelRequestRepository cancelRequestRepository;
    private final SeatHoldRedisService seatHoldRedisService;
    private final CurrentUserService currentUserService;
    private final CancellationPolicyService policyService;
    private final CancellationPreviewManager previewManager;
    private final VietnameseLocationNormalizer locationNormalizer = new VietnameseLocationNormalizer();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Tool(name = "findMyBookings", description = "Tìm danh sách đơn đặt vé của khách hàng đang đăng nhập theo từ khóa tuyến xe hoặc thời gian.")
    public List<MyBookingSummaryDto> findMyBookings(
            @ToolParam(description = "Từ khóa tìm kiếm (tên điểm đến, điểm đi, hoặc ngày)", required = false) String keyword) {
        return findMyBookingsFiltered(null, null, null, keyword);
    }

    @Tool(name = "findMyBookingsByDestination", description = "Tìm danh sách đơn đặt vé của khách hàng đang đăng nhập theo điểm đến.")
    public List<MyBookingSummaryDto> findMyBookingsByDestination(
            @ToolParam(description = "Điểm đến cần tra cứu (ví dụ: Đà Lạt, Nha Trang)") String destination) {
        return findMyBookingsFiltered(null, destination, null, null);
    }

    public List<MyBookingSummaryDto> findMyBookingsFiltered(
            String departureLocation,
            String arrivalLocation,
            LocalDate departureDate,
            String keyword) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để tra cứu đơn đặt vé của mình!", HttpStatus.UNAUTHORIZED));

        List<Booking> bookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId());
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        String depNorm = (departureLocation != null && !departureLocation.isBlank()) ? locationNormalizer.normalize(departureLocation) : null;
        String arrNorm = (arrivalLocation != null && !arrivalLocation.isBlank()) ? locationNormalizer.normalize(arrivalLocation) : null;
        String kw = keyword != null ? keyword.toLowerCase(Locale.ROOT).trim() : null;
        List<MyBookingSummaryDto> result = new ArrayList<>();

        for (Booking booking : bookings) {
            // Exclude CANCELLED bookings when looking for active or cancellable bookings
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                continue;
            }

            String dep = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                    ? booking.getTrip().getRoute().getDepartureLocation() : "";
            String arr = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                    ? booking.getTrip().getRoute().getArrivalLocation() : "";
            String depNormActual = locationNormalizer.normalize(dep);
            String arrNormActual = locationNormalizer.normalize(arr);

            if (depNorm != null && !depNorm.isBlank()) {
                if (!depNormActual.contains(depNorm) && !depNorm.contains(depNormActual)) {
                    continue;
                }
            }

            if (arrNorm != null && !arrNorm.isBlank()) {
                if (!arrNormActual.contains(arrNorm) && !arrNorm.contains(arrNormActual)) {
                    continue;
                }
            }

            LocalDateTime depTime = (booking.getTrip() != null) ? booking.getTrip().getDepartureTime() : null;
            if (departureDate != null && depTime != null) {
                // Must use trip departure date, NEVER booking.getCreatedAt()!
                if (!depTime.toLocalDate().isEqual(departureDate)) {
                    continue;
                }
            }

            String depTimeFormatted = depTime != null ? depTime.format(TIME_FORMATTER) : "N/A";
            String routeName = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                    ? dep + " -> " + arr
                    : "Chuyến #" + (booking.getTrip() != null ? booking.getTrip().getId() : "N/A");

            if (kw != null && !kw.isBlank()) {
                boolean matchRoute = routeName.toLowerCase(Locale.ROOT).contains(kw)
                        || dep.toLowerCase(Locale.ROOT).contains(kw)
                        || arr.toLowerCase(Locale.ROOT).contains(kw)
                        || depNormActual.contains(kw)
                        || arrNormActual.contains(kw);
                boolean matchTime = depTimeFormatted.toLowerCase(Locale.ROOT).contains(kw);
                boolean matchCode = booking.getBookingCode() != null && booking.getBookingCode().toLowerCase(Locale.ROOT).contains(kw);
                boolean matchId = String.valueOf(booking.getId()).equals(kw);

                if (!matchRoute && !matchTime && !matchCode && !matchId) {
                    continue;
                }
            }

            CancellationEligibilityDto eligibility = policyService.evaluateEligibility(booking, null);

            result.add(MyBookingSummaryDto.builder()
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .tripId(booking.getTrip() != null ? booking.getTrip().getId() : null)
                    .routeName(routeName)
                    .departureLocation(dep)
                    .arrivalLocation(arr)
                    .departureTime(depTime)
                    .departureTimeFormatted(depTimeFormatted)
                    .vehicleType(booking.getTrip() != null && booking.getTrip().getVehicle() != null ? booking.getTrip().getVehicle().getType() : "")
                    .seatNames(booking.getSeats())
                    .ticketCount(booking.getSeats() != null ? booking.getSeats().size() : 0)
                    .totalPrice(booking.getTotalPrice())
                    .totalPriceFormatted(CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ")
                    .status(booking.getStatus())
                    .isCancellable(eligibility.isEligible())
                    .nonCancellableReason(eligibility.getReason())
                    .build());
        }

        return result;
    }

    @Tool(name = "getBookingDetails", description = "Lấy thông tin chi tiết một đơn đặt vé và danh sách các vé bên trong của khách hàng.")
    public Map<String, Object> getBookingDetails(
            @ToolParam(description = "Mã ID đơn đặt vé", required = false) Long bookingId,
            @ToolParam(description = "Mã đặt vé BookingCode", required = false) String bookingCode) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để thao tác!", HttpStatus.UNAUTHORIZED));

        Booking booking = null;
        if (bookingId != null) {
            booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId()).orElse(null);
        }
        if (booking == null && bookingCode != null && !bookingCode.isBlank()) {
            List<Booking> found = bookingRepository.findDistinctByTickets_TicketCode(bookingCode.trim())
                    .map(List::of).orElse(Collections.emptyList());
            if (!found.isEmpty() && found.get(0).getCustomer().getId().equals(customer.getId())) {
                booking = found.get(0);
            }
        }

        if (booking == null) {
            throw new CustomException("Không tìm thấy đơn đặt vé hợp lệ của quý khách!", HttpStatus.NOT_FOUND);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bookingId", booking.getId());
        res.put("bookingCode", booking.getBookingCode());
        res.put("status", booking.getStatus());
        res.put("totalPrice", booking.getTotalPrice());
        res.put("totalPriceFormatted", CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ");

        if (booking.getTrip() != null) {
            res.put("tripId", booking.getTrip().getId());
            if (booking.getTrip().getRoute() != null) {
                res.put("routeName", booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation());
            }
            if (booking.getTrip().getDepartureTime() != null) {
                res.put("departureTimeFormatted", booking.getTrip().getDepartureTime().format(TIME_FORMATTER));
            }
        }

        List<Map<String, Object>> ticketList = new ArrayList<>();
        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                Map<String, Object> tMap = new LinkedHashMap<>();
                tMap.put("ticketId", t.getId());
                tMap.put("ticketCode", t.getTicketCode());
                tMap.put("seatName", t.getSeat() != null ? t.getSeat().getSeatName() : "N/A");
                tMap.put("floor", t.getSeat() != null ? t.getSeat().getFloor() : 1);
                tMap.put("price", t.getPrice());
                tMap.put("priceFormatted", CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ");
                tMap.put("status", t.getStatus());
                ticketList.add(tMap);
            }
        }
        res.put("tickets", ticketList);
        return res;
    }

    @Tool(name = "checkCancellationEligibility", description = "Kiểm tra điều kiện hủy vé, chính sách phí hủy và số tiền hoàn dự kiến.")
    public CancellationEligibilityDto checkCancellationEligibility(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId,
            @ToolParam(description = "Danh sách ID các vé muốn hủy (bỏ trống nếu hủy toàn bộ)", required = false) List<Long> ticketIds) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để kiểm tra điều kiện hủy vé!", HttpStatus.UNAUTHORIZED));

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId + " thuộc tài khoản của quý khách!", HttpStatus.NOT_FOUND));

        return policyService.evaluateEligibility(booking, ticketIds);
    }

    @Tool(name = "previewCancellation", description = "Tạo bản tóm tắt xác nhận hủy vé (Cancellation Preview) kèm số tiền hoàn, phí hủy và token xác nhận có thời hạn 5 phút.")
    public CancellationPreviewDto previewCancellation(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId,
            @ToolParam(description = "Danh sách ID các vé muốn hủy (bỏ trống nếu hủy toàn bộ)", required = false) List<Long> ticketIds,
            @ToolParam(description = "Lý do hủy vé", required = false) String cancelReason) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để chuẩn bị hủy vé!", HttpStatus.UNAUTHORIZED));

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId + " thuộc tài khoản của quý khách!", HttpStatus.NOT_FOUND));

        CancellationEligibilityDto eligibility = policyService.evaluateEligibility(booking, ticketIds);
        if (!eligibility.isEligible()) {
            throw new CustomException("Không đủ điều kiện hủy vé: " + eligibility.getReason(), HttpStatus.BAD_REQUEST);
        }

        List<Long> actualTicketIds = eligibility.getCancellableTicketIds();
        List<String> actualSeatNames = eligibility.getCancellableSeatNames();

        List<String> remainingSeats = new ArrayList<>();
        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                if (!actualTicketIds.contains(t.getId()) && !"CANCELLED".equalsIgnoreCase(t.getStatus())) {
                    if (t.getSeat() != null) remainingSeats.add(t.getSeat().getSeatName());
                }
            }
        }

        boolean isPartial = !remainingSeats.isEmpty();
        String routeName = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                ? booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation()
                : "Chuyến #" + (booking.getTrip() != null ? booking.getTrip().getId() : "N/A");

        String depTimeStr = (booking.getTrip() != null && booking.getTrip().getDepartureTime() != null)
                ? booking.getTrip().getDepartureTime().format(TIME_FORMATTER) : "N/A";

        String confirmationToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        BigDecimal originalAmount = eligibility.getRefundAmount().add(eligibility.getCancellationFee());

        String summary = String.format(
                "📋 **BẢN TÓM TẮT HỦY VÉ CHỜ XÁC NHẬN:**\n\n" +
                "- **Mã đơn đặt vé**: `%s` (ID: #%d)\n" +
                "- **Tuyến xe**: %s\n" +
                "- **Khởi hành**: %s\n" +
                "- **Ghế yêu cầu hủy**: **%s** (%d vé %s)\n" +
                (isPartial ? "- **Ghế tiếp tục giữ lại**: `" + String.join(", ", remainingSeats) + "`\n" : "") +
                "- **Tổng giá vé hủy**: %s\n" +
                "- **Phí hủy áp dụng**: **%s** (%d%% theo chính sách)\n" +
                "- **Số tiền hoàn lại**: **%s**\n" +
                "- **Chính sách**: *%s*\n\n" +
                "⚠️ *Bản xác nhận này có hiệu lực trong vòng **5 phút**. Quý khách vui lòng nhấn nút **'Xác nhận hủy vé'** để hoàn tất.*",
                booking.getBookingCode(), booking.getId(),
                routeName, depTimeStr,
                String.join(", ", actualSeatNames),
                actualSeatNames.size(),
                isPartial ? "(Hủy một phần)" : "(Hủy toàn bộ)",
                CURRENCY_FORMATTER.format(originalAmount) + " VNĐ",
                eligibility.getCancellationFeeFormatted(), eligibility.getFeePercent(),
                eligibility.getRefundAmountFormatted(),
                eligibility.getPolicyDescription()
        );

        CancellationPreviewDto preview = CancellationPreviewDto.builder()
                .confirmationToken(confirmationToken)
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerId(customer.getId())
                .tripId(booking.getTrip() != null ? booking.getTrip().getId() : null)
                .routeName(routeName)
                .departureTimeFormatted(depTimeStr)
                .vehicleType(booking.getTrip() != null && booking.getTrip().getVehicle() != null ? booking.getTrip().getVehicle().getType() : "")
                .ticketIdsToCancel(actualTicketIds)
                .seatNamesToCancel(actualSeatNames)
                .ticketCountToCancel(actualTicketIds.size())
                .isPartialCancellation(isPartial)
                .remainingSeatNames(remainingSeats)
                .originalAmount(originalAmount)
                .originalAmountFormatted(CURRENCY_FORMATTER.format(originalAmount) + " VNĐ")
                .feePercent(eligibility.getFeePercent())
                .feeAmount(eligibility.getCancellationFee())
                .feeAmountFormatted(eligibility.getCancellationFeeFormatted())
                .refundAmount(eligibility.getRefundAmount())
                .refundAmountFormatted(eligibility.getRefundAmountFormatted())
                .refundMethod(eligibility.isPaid() ? "DEMO_SIMULATION" : "NONE")
                .cancellationPolicy(eligibility.getPolicyDescription())
                .expiresAt(expiresAt)
                .expiresInSeconds(300)
                .summaryMessage(summary)
                .build();

        previewManager.savePreview(preview);
        return preview;
    }

    @Tool(name = "confirmCancellation", description = "Chính thức thực hiện hủy vé sau khi người dùng ĐÃ XÁC NHẬN RÕ RÀNG qua token xác nhận.")
    @Transactional
    public CancellationResultDto confirmCancellation(
            @ToolParam(description = "Token xác nhận confirmationToken từ bước previewCancellation") String confirmationToken,
            @ToolParam(description = "Mã ID đơn đặt vé", required = false) Long bookingId,
            @ToolParam(description = "Danh sách ID các vé hủy", required = false) List<Long> ticketIds) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để hoàn tất hủy vé!", HttpStatus.UNAUTHORIZED));

        // Check idempotency (prevent double-click & duplicate refund)
        if (confirmationToken != null && !confirmationToken.isBlank()) {
            Optional<CancellationResultDto> existing = previewManager.getExecutedResult(confirmationToken);
            if (existing.isPresent()) {
                log.info("[Cancellation] Idempotent hit for token {}. Returning previous cancellation result.", confirmationToken);
                CancellationResultDto res = existing.get();
                res.setAlreadyExecuted(true);
                return res;
            }
        }

        CancellationPreviewDto preview = previewManager.getPreview(confirmationToken, customer.getId())
                .orElseGet(() -> previewManager.getLatestPreviewForCustomer(customer.getId())
                        .orElseThrow(() -> new CustomException("Bản tóm tắt hủy vé đã hết hạn hoặc không tồn tại. Quý khách vui lòng kiểm tra lại!", HttpStatus.GONE)));

        Long bId = (bookingId != null) ? bookingId : preview.getBookingId();
        Booking booking = bookingRepository.findByIdAndCustomerId(bId, customer.getId())
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé thuộc tài khoản của bạn!", HttpStatus.NOT_FOUND));

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new CustomException("Đơn đặt vé này đã bị hủy trước đó!", HttpStatus.BAD_REQUEST);
        }

        List<Long> toCancelIds = preview.getTicketIdsToCancel();
        List<Ticket> ticketsToCancel = new ArrayList<>();
        List<String> cancelledSeats = new ArrayList<>();
        List<String> remainingSeats = new ArrayList<>();

        if (booking.getTickets() != null) {
            for (Ticket t : booking.getTickets()) {
                if (toCancelIds.contains(t.getId())) {
                    if ("USED".equalsIgnoreCase(t.getStatus())) {
                        throw new CustomException("Vé ghế " + (t.getSeat() != null ? t.getSeat().getSeatName() : "N/A") + " đã được sử dụng, không thể hủy!", HttpStatus.BAD_REQUEST);
                    }
                    t.cancel();
                    ticketsToCancel.add(t);
                    if (t.getSeat() != null) cancelledSeats.add(t.getSeat().getSeatName());
                } else if (!"CANCELLED".equalsIgnoreCase(t.getStatus())) {
                    if (t.getSeat() != null) remainingSeats.add(t.getSeat().getSeatName());
                }
            }
            ticketRepository.saveAll(ticketsToCancel);
        }

        boolean isPartial = !remainingSeats.isEmpty();
        if (isPartial) {
            booking.setStatus("PARTIALLY_CANCELLED");
        } else {
            booking.cancelBooking();
            seatHoldRedisService.releaseHoldByBookingId(booking.getId());
        }
        bookingRepository.save(booking);

        // Record CancelRequest history
        try {
            CancelRequest cr = CancelRequest.builder()
                    .booking(booking)
                    .status("APPROVED")
                    .rejectReason("Hủy thành công qua Trợ lý AI Chatbot: " + (isPartial ? "Hủy một phần ghế: " + String.join(", ", cancelledSeats) : "Hủy toàn bộ đơn"))
                    .resolvedAt(LocalDateTime.now())
                    .build();
            cancelRequestRepository.save(cr);
        } catch (Exception e) {
            log.warn("Notice: CancelRequest log record notice: {}", e.getMessage());
        }

        // Demo Refund Simulation
        String refundStatus = "NO_REFUND_UNPAID";
        String refundTxnId = null;
        String refundMethod = preview.getRefundMethod();

        if ("PAID".equalsIgnoreCase(booking.getStatus()) || "PARTIALLY_CANCELLED".equalsIgnoreCase(booking.getStatus()) || preview.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Payment> paymentOpt = paymentRepository.findByBookingId(booking.getId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                refundTxnId = "DEMO_REFUND_" + booking.getId() + "_" + System.currentTimeMillis();
                payment.setStatus("REFUNDED");
                payment.setTransactionId(refundTxnId);
                paymentRepository.save(payment);
                refundStatus = "REFUNDED";
                refundMethod = "DEMO_INSTANT";
            } else {
                refundStatus = "REFUNDED";
                refundTxnId = "DEMO_REFUND_" + booking.getId() + "_" + System.currentTimeMillis();
                refundMethod = "DEMO_INSTANT";
            }
        }

        LocalDateTime now = LocalDateTime.now();
        CancellationResultDto result = CancellationResultDto.builder()
                .success(true)
                .message("Hủy vé thành công! Ghế " + String.join(", ", cancelledSeats) + " đã được giải phóng trở lại.")
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .bookingStatus(booking.getStatus())
                .cancelledSeats(cancelledSeats)
                .remainingSeats(remainingSeats)
                .isPartialCancellation(isPartial)
                .refundAmount(preview.getRefundAmount())
                .refundAmountFormatted(preview.getRefundAmountFormatted())
                .cancellationFee(preview.getFeeAmount())
                .cancellationFeeFormatted(preview.getFeeAmountFormatted())
                .refundStatus(refundStatus)
                .refundTransactionId(refundTxnId)
                .refundMethod(refundMethod)
                .cancelledAt(now)
                .cancelledAtFormatted(now.format(TIME_FORMATTER))
                .alreadyExecuted(false)
                .build();

        previewManager.registerExecutedResult(preview.getConfirmationToken(), result);
        return result;
    }

    @Tool(name = "getCancellationStatus", description = "Tra cứu tình trạng hủy vé và thông tin hoàn tiền của một đơn đặt vé.")
    public Map<String, Object> getCancellationStatus(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId) {

        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập để tra cứu!", HttpStatus.UNAUTHORIZED));

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId, HttpStatus.NOT_FOUND));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bookingId", booking.getId());
        res.put("bookingCode", booking.getBookingCode());
        res.put("bookingStatus", booking.getStatus());
        res.put("seats", booking.getSeats());

        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);
        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            res.put("paymentStatus", p.getStatus());
            res.put("paymentTransactionId", p.getTransactionId());
        }

        return res;
    }

    public BookingRepository getBookingRepository() {
        return bookingRepository;
    }
}
