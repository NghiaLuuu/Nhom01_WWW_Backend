package vn.edu.iuh.nhom01_www.chatbot.cancellation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.CancellationEligibilityDto;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class CancellationPolicyService {

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final Clock clock;

    public CancellationPolicyService() { this(Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))); }
    public CancellationPolicyService(Clock clock) { this.clock = clock; }

    public CancellationEligibilityDto evaluateEligibility(Booking booking, List<Long> requestedTicketIds) {
        if (booking == null) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .reason("Không tìm thấy thông tin đơn đặt vé!")
                    .build();
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .reason("Đơn đặt vé này đã bị hủy trước đó.")
                    .build();
        }

        // Check if booking is PENDING_PAYMENT
        if ("PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
            List<Ticket> activeTickets = getActiveTickets(booking, requestedTicketIds);
            if (activeTickets.isEmpty() || activeTickets.stream().anyMatch(t -> "USED".equalsIgnoreCase(t.getStatus()))) {
                return CancellationEligibilityDto.builder().eligible(false).bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode()).reason("Không có vé hợp lệ nào có thể hủy trong đơn này.").build();
            }
            List<Long> cancellableIds = activeTickets.stream().map(Ticket::getId).toList();
            List<String> cancellableSeats = activeTickets.stream()
                    .map(t -> t.getSeat() != null ? t.getSeat().getSeatName() : "N/A")
                    .toList();

            return CancellationEligibilityDto.builder()
                    .eligible(true)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .hoursUntilDeparture(0)
                    .feePercent(0)
                    .cancellationFee(BigDecimal.ZERO)
                    .cancellationFeeFormatted("0 VNĐ")
                    .refundAmount(BigDecimal.ZERO)
                    .refundAmountFormatted("0 VNĐ")
                    .policyDescription("Đơn đặt vé chưa thanh toán: Hủy ngay lập tức và giải phóng ghế miễn phí.")
                    .isPaid(false)
                    .requiresStaffApproval(false)
                    .cancellableTicketIds(cancellableIds)
                    .cancellableSeatNames(cancellableSeats)
                    .build();
        }

        if (!"PAID".equalsIgnoreCase(booking.getStatus()) && !"PARTIALLY_CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .reason("Trạng thái đơn đặt vé (" + booking.getStatus() + ") không hợp lệ để thực hiện hủy.")
                    .build();
        }

        // Check trip departure time
        if (booking.getTrip() == null || booking.getTrip().getDepartureTime() == null) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .reason("Không xác định được lịch trình chuyến xe!")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime departureTime = booking.getTrip().getDepartureTime();

        if (!now.isBefore(departureTime)) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .reason("Chuyến xe đã khởi hành! Không thể thực hiện hủy vé sau giờ khởi hành.")
                    .build();
        }

        Duration remaining = Duration.between(now, departureTime);
        double hoursUntilDeparture = remaining.toMillis() / 3600000.0;

        // Check ticket status
        List<Ticket> activeTickets = getActiveTickets(booking, requestedTicketIds);
        if (activeTickets.isEmpty()) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .reason("Không có vé hợp lệ nào có thể hủy trong đơn này (các vé đã sử dụng hoặc đã hủy).")
                    .build();
        }

        for (Ticket ticket : activeTickets) {
            if ("USED".equalsIgnoreCase(ticket.getStatus())) {
                return CancellationEligibilityDto.builder()
                        .eligible(false)
                        .bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .reason("Vé (Ghế: " + (ticket.getSeat() != null ? ticket.getSeat().getSeatName() : "N/A") + ") đã được sử dụng để lên xe, không thể hủy!")
                        .build();
            }
        }

        // Calculate fee percent based on time before departure
        int feePercent = remaining.compareTo(Duration.ofHours(2)) > 0 ? 20 : 100;
        String policyDescription = feePercent == 20
                ? "Hủy trước giờ khởi hành hơn 2 giờ: Phí hủy 20%, hoàn tiền 80%."
                : "Hủy trong vòng 2 giờ trước giờ khởi hành: Không hoàn tiền.";

        BigDecimal originalAmount = activeTickets.stream()
                .map(Ticket::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cancellationFee = originalAmount.multiply(BigDecimal.valueOf(feePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal refundAmount = originalAmount.subtract(cancellationFee);

        List<Long> cancellableIds = activeTickets.stream().map(Ticket::getId).toList();
        List<String> cancellableSeats = activeTickets.stream()
                .map(t -> t.getSeat() != null ? t.getSeat().getSeatName() : "N/A")
                .toList();

        return CancellationEligibilityDto.builder()
                .eligible(true)
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .hoursUntilDeparture(hoursUntilDeparture)
                .feePercent(feePercent)
                .cancellationFee(cancellationFee)
                .cancellationFeeFormatted(CURRENCY_FORMATTER.format(cancellationFee) + " VNĐ")
                .refundAmount(refundAmount)
                .refundAmountFormatted(CURRENCY_FORMATTER.format(refundAmount) + " VNĐ")
                .policyDescription(policyDescription)
                .isPaid(true)
                .requiresStaffApproval(false)
                .cancellableTicketIds(cancellableIds)
                .cancellableSeatNames(cancellableSeats)
                .build();
    }

    private List<Ticket> getActiveTickets(Booking booking, List<Long> requestedTicketIds) {
        if (booking.getTickets() == null) return Collections.emptyList();
        return booking.getTickets().stream()
                .filter(t -> !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> !"NO_SHOW".equalsIgnoreCase(t.getStatus()) ||
                        (!"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus()) && booking.getTrip() != null
                                && booking.getTrip().getDepartureTime() != null
                                && LocalDateTime.now(clock).isBefore(booking.getTrip().getDepartureTime())))
                .filter(t -> requestedTicketIds == null || requestedTicketIds.isEmpty() || requestedTicketIds.contains(t.getId()))
                .toList();
    }
}
