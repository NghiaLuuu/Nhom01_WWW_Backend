package vn.edu.iuh.nhom01_www.payment.demo.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.util.QRCodeGenerator;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoTicketDto;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoPaymentProcessor implements PaymentProcessor {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRedisService seatHoldRedisService;

    @Value("${app.demo-payment.enabled:false}")
    private boolean demoPaymentEnabled;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    public static final String METHOD_DEMO = "DEMO_SIMULATION";

    @Override
    public boolean supports(String paymentMethod) {
        return METHOD_DEMO.equalsIgnoreCase(paymentMethod);
    }

    private void verifyDemoEnabled() {
        if (!demoPaymentEnabled) {
            log.error("[DemoPayment] Demo payment requested but app.demo-payment.enabled is false (Production guard active)");
            throw new CustomException("Cổng thanh toán mô phỏng (Demo) bị vô hiệu hóa trong môi trường này!", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    @Transactional
    public DemoPaymentQrResponse createDemoPayment(Long bookingId, UUID customerId) {
        verifyDemoEnabled();

        if (bookingId == null) {
            throw new CustomException("Mã bookingId không được để trống!", HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId, HttpStatus.NOT_FOUND));

        // Check ownership
        if (customerId != null && booking.getCustomer() != null && !booking.getCustomer().getId().equals(customerId)) {
            throw new CustomException("Quý khách không có quyền thao tác trên đơn đặt vé này!", HttpStatus.FORBIDDEN);
        }

        // Validate booking status
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Đơn đặt vé không ở trạng thái chờ thanh toán (Trạng thái hiện tại: " + booking.getStatus() + ")!", HttpStatus.BAD_REQUEST);
        }

        // Validate Redis seat hold is active
        if (!seatHoldRedisService.isBookingHoldActive(bookingId)) {
            throw new CustomException("Thời hạn giữ chỗ 5 phút cho đơn đặt vé này đã hết hạn. Ghế đã được giải phóng!", HttpStatus.GONE);
        }

        String transactionId = "DEMO_TXN_" + bookingId + "_" + System.currentTimeMillis();

        // Save or update pending payment record
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalPrice())
                        .paymentMethod(METHOD_DEMO)
                        .status("PENDING")
                        .transactionId(transactionId)
                        .build());

        payment.setTransactionId(transactionId);
        payment.setPaymentMethod(METHOD_DEMO);
        payment.setStatus("PENDING");
        payment.setAmount(booking.getTotalPrice());
        paymentRepository.save(payment);

        // Generate Demo QR payload
        String qrPayload = String.format("DEMO_PAYMENT|BOOKING:%d|TXN:%s|AMOUNT:%s|STATUS:PENDING|NOTICE:INTERNAL_DEMO_NO_REAL_MONEY",
                booking.getId(),
                transactionId,
                booking.getTotalPrice().toString());

        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300);

        String routeName = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                ? booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation()
                : "Chuyến #" + (booking.getTrip() != null ? booking.getTrip().getId() : "N/A");

        String departureTimeStr = (booking.getTrip() != null && booking.getTrip().getDepartureTime() != null)
                ? booking.getTrip().getDepartureTime().format(TIME_FORMATTER)
                : "";

        List<String> seatNames = booking.getSeats();

        log.info("[DemoPayment:create] Generated Demo Payment QR for booking #{}, txn={}", bookingId, transactionId);

        return DemoPaymentQrResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .transactionId(transactionId)
                .amount(booking.getTotalPrice())
                .amountFormatted(CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ")
                .qrPayload(qrPayload)
                .qrBase64(qrBase64)
                .expiresInSeconds(300)
                .expiresAt(LocalDateTime.now().plusSeconds(300))
                .status("PENDING")
                .routeName(routeName)
                .departureTimeFormatted(departureTimeStr)
                .seatNames(seatNames)
                .demoNote("THANH TOÁN MÔ PHỎNG — KHÔNG CHUYỂN TIỀN THẬT")
                .build();
    }

    @Override
    @Transactional
    public DemoPaymentConfirmResponse processPayment(Long bookingId, String transactionId, UUID customerId) {
        verifyDemoEnabled();

        if (bookingId == null) {
            throw new CustomException("Mã bookingId không được để trống!", HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId, HttpStatus.NOT_FOUND));

        // Check ownership
        if (customerId != null && booking.getCustomer() != null && !booking.getCustomer().getId().equals(customerId)) {
            throw new CustomException("Quý khách không có quyền thanh toán đơn đặt vé này!", HttpStatus.FORBIDDEN);
        }

        // Idempotency: If booking is already PAID, return success immediately without re-processing
        if ("PAID".equals(booking.getStatus())) {
            log.info("[DemoPayment:confirm] Idempotent hit: Booking #{} is already PAID. Returning existing tickets.", bookingId);
            return buildSuccessResponse(booking, transactionId != null ? transactionId : "DEMO_IDEMPOTENT_" + bookingId, "Đơn đặt vé đã được thanh toán thành công trước đó!");
        }

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Đơn đặt vé không hợp lệ để thanh toán (Trạng thái: " + booking.getStatus() + ")!", HttpStatus.BAD_REQUEST);
        }

        // Check Redis seat hold expiration
        if (!seatHoldRedisService.isBookingHoldActive(bookingId)) {
            booking.cancelBooking();
            bookingRepository.save(booking);
            throw new CustomException("Đơn đặt vé đã hết hạn giữ chỗ và bị hủy tự động. Vui lòng đặt lại vé mới!", HttpStatus.GONE);
        }

        // Process payment
        String finalTxnId = (transactionId != null && !transactionId.isBlank())
                ? transactionId
                : "DEMO_TXN_" + bookingId + "_" + System.currentTimeMillis();

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalPrice())
                        .paymentMethod(METHOD_DEMO)
                        .build());

        payment.setStatus("SIMULATED_PAID");
        payment.setPaymentMethod(METHOD_DEMO);
        payment.setTransactionId(finalTxnId);
        payment.setAmount(booking.getTotalPrice());
        paymentRepository.save(payment);

        // Update booking state
        booking.markAsPaid();

        // Issue tickets and generate dedicated electronic ticket QR codes
        List<DemoTicketDto> ticketDtos = new ArrayList<>();
        if (booking.getTickets() != null) {
            for (Ticket ticket : booking.getTickets()) {
                ticket.issue();
                if (ticket.getTicketCode() == null || ticket.getTicketCode().isBlank()) {
                    ticket.setTicketCode("VXDEMO" + System.currentTimeMillis() + ticket.getId());
                }

                // Dedicated ticket check-in QR code (distinct from demo payment QR)
                String ticketQrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                        ticket.getTicketCode(),
                        booking.getId(),
                        ticket.getSeat() != null ? ticket.getSeat().getSeatName() : "N/A",
                        ticket.getPrice().toString());

                String ticketQrBase64 = QRCodeGenerator.generateQRCodeBase64(ticketQrPayload, 300, 300);
                ticket.setQrCode(ticketQrBase64);

                ticketDtos.add(DemoTicketDto.builder()
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .seatName(ticket.getSeat() != null ? ticket.getSeat().getSeatName() : "")
                        .floor(ticket.getSeat() != null ? ticket.getSeat().getFloor() : 1)
                        .price(ticket.getPrice())
                        .priceFormatted(CURRENCY_FORMATTER.format(ticket.getPrice()) + " VNĐ")
                        .status("ISSUED")
                        .qrPayload(ticketQrPayload)
                        .qrCodeBase64(ticketQrBase64)
                        .build());
            }
            ticketRepository.saveAll(booking.getTickets());
        }

        bookingRepository.save(booking);

        // Release temporary Redis seat hold because seats are now permanently purchased in DB!
        seatHoldRedisService.releaseHoldByBookingId(bookingId);

        log.info("[DemoPayment:confirm] SUCCESS for booking #{}, issued {} tickets. Payment status=SIMULATED_PAID",
                bookingId, ticketDtos.size());

        return buildSuccessResponse(booking, finalTxnId, "Mô phỏng thanh toán thành công! Vé điện tử đã được phát hành.");
    }

    @Override
    @Transactional
    public void cancelPayment(Long bookingId, UUID customerId) {
        verifyDemoEnabled();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId, HttpStatus.NOT_FOUND));

        if (customerId != null && booking.getCustomer() != null && !booking.getCustomer().getId().equals(customerId)) {
            throw new CustomException("Quý khách không có quyền thao tác trên đơn đặt vé này!", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Chỉ có thể hủy đơn đặt vé đang chờ thanh toán!", HttpStatus.BAD_REQUEST);
        }

        booking.cancelBooking();
        seatHoldRedisService.releaseHoldByBookingId(bookingId);
        if (booking.getTickets() != null) {
            for (Ticket ticket : booking.getTickets()) {
                ticket.cancel();
            }
            ticketRepository.saveAll(booking.getTickets());
        }
        bookingRepository.save(booking);

        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            p.setStatus("CANCELLED");
            paymentRepository.save(p);
        });

        log.info("[DemoPayment:cancel] Cancelled booking #{} and released held seats.", bookingId);
    }

    private DemoPaymentConfirmResponse buildSuccessResponse(Booking booking, String txnId, String message) {
        String routeName = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                ? booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation()
                : "Chuyến #" + (booking.getTrip() != null ? booking.getTrip().getId() : "N/A");

        String departureTimeStr = (booking.getTrip() != null && booking.getTrip().getDepartureTime() != null)
                ? booking.getTrip().getDepartureTime().format(TIME_FORMATTER)
                : "";

        List<DemoTicketDto> ticketDtos = new ArrayList<>();
        if (booking.getTickets() != null) {
            for (Ticket ticket : booking.getTickets()) {
                ticketDtos.add(DemoTicketDto.builder()
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .seatName(ticket.getSeat() != null ? ticket.getSeat().getSeatName() : "")
                        .floor(ticket.getSeat() != null ? ticket.getSeat().getFloor() : 1)
                        .price(ticket.getPrice())
                        .priceFormatted(CURRENCY_FORMATTER.format(ticket.getPrice()) + " VNĐ")
                        .status(ticket.getStatus())
                        .qrPayload("TICKET:" + ticket.getTicketCode() + "|BOOKING:" + booking.getId() + "|SEAT:" + (ticket.getSeat() != null ? ticket.getSeat().getSeatName() : "N/A"))
                        .qrCodeBase64(ticket.getQrCode())
                        .build());
            }
        }

        return DemoPaymentConfirmResponse.builder()
                .success(true)
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status("PAID")
                .totalPrice(booking.getTotalPrice())
                .totalPriceFormatted(CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ")
                .paymentMethod(METHOD_DEMO)
                .transactionId(txnId)
                .routeName(routeName)
                .departureTimeFormatted(departureTimeStr)
                .tickets(ticketDtos)
                .message(message)
                .build();
    }
}
