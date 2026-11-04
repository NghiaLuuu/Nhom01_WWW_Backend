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
import java.util.*;

@Slf4j
@Service
public class CancellationPolicyService {

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = booking.getTrip().getDepartureTime();

        if (now.isAfter(departureTime)) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .reason("Chuyến xe đã khởi hành! Không thể thực hiện hủy vé sau giờ khởi hành.")
                    .build();
        }

        double hoursUntilDeparture = Duration.between(now, departureTime).toMinutes() / 60.0;
        if (hoursUntilDeparture < 2.0) {
            return CancellationEligibilityDto.builder()
                    .eligible(false)
                    .bookingId(booking.getId())
                    .bookingCode(booking.getBookingCode())
                    .hoursUntilDeparture(hoursUntilDeparture)
                    .reason("Đã quá thời hạn hủy vé trực tuyến (phải trước giờ khởi hành tối thiểu 2 giờ).")
                    .build();
        }

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
        int feePercent;
        String policyDescription;
        if (hoursUntilDeparture >= 24.0) {
            feePercent = 10;
            policyDescription = "Hủy trước 24 giờ so với giờ khởi hành: Phí hủy 10%, hoàn tiền 90%.";
        } else if (hoursUntilDeparture >= 12.0) {
            feePercent = 20;
            policyDescription = "Hủy từ 12 đến dưới 24 giờ trước khởi hành: Phí hủy 20%, hoàn tiền 80%.";
        } else {
            feePercent = 50;
            policyDescription = "Hủy từ 2 đến dưới 12 giờ trước khởi hành: Phí hủy 50%, hoàn tiền 50%.";
        }

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
                .filter(t -> requestedTicketIds == null || requestedTicketIds.isEmpty() || requestedTicketIds.contains(t.getId()))
                .toList();
    }
}
