package vn.edu.iuh.nhom01_www.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.service.EmailService;
import vn.edu.iuh.nhom01_www.common.util.QRCodeGenerator;
import vn.edu.iuh.nhom01_www.payment.config.VNPayConfig;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.payment.service.PaymentService;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final VNPayConfig vnPayConfig;
    private final EmailService emailService;
    private final SeatHoldRedisService seatHoldRedisService;

    @Override
    @Transactional(readOnly = true)
    public String createVNPayPaymentUrl(Long bookingId, String ipAddress, UUID customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé!", HttpStatus.NOT_FOUND));

        if (customerId != null && !booking.getCustomer().getId().equals(customerId)) {
            throw new CustomException("Bạn không có quyền thanh toán đơn đặt vé này!", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Đơn đặt vé không ở trạng thái chờ thanh toán (Trạng thái hiện tại: " + booking.getStatus() + ")", HttpStatus.BAD_REQUEST);
        }

        if (!seatHoldRedisService.isBookingHoldActive(bookingId)) {
            throw new CustomException("Đơn đặt vé đã hết hạn giữ chỗ (5 phút). Vui lòng đặt lại vé mới!", HttpStatus.BAD_REQUEST);
        }

        long amount = booking.getTotalPrice().multiply(new BigDecimal(100)).longValue();
        String vnp_TxnRef = bookingId + "_" + System.currentTimeMillis();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String vnp_CreateDate = formatter.format(now);

        LocalDateTime expireTime = seatHoldRedisService.getHoldExpiresAt(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy thời hạn giữ chỗ trên Redis. Vui lòng đặt lại vé mới!", HttpStatus.BAD_REQUEST));
        String vnp_ExpireDate = formatter.format(expireTime);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
        vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan ve xe don hang #" + bookingId);
        vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnp_Params.put("vnp_Locale", vnPayConfig.getLocale());
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1");
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build query string and compute hash
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    hashData.append(fieldValue);
                    query.append(fieldName).append('=').append(fieldValue);
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getUrl() + "?" + queryUrl;
    }

    @Override
    @Transactional
    public Payment processVNPayCallback(Map<String, String> vnpParams) {
        log.info("Xử lý Callback VNPay với params: {}", vnpParams);
        
        Map<String, String> fields = new HashMap<>(vnpParams);
        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String signValue = vnPayConfig.hashAllFields(fields);
        if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
            log.error("Chữ ký VNPay không hợp lệ! Kỳ vọng: {}, Thực tế: {}", signValue, vnp_SecureHash);
            throw new CustomException("Chữ ký bảo mật VNPay không hợp lệ (Invalid Checksum)!", HttpStatus.BAD_REQUEST);
        }

        String txnRef = fields.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new CustomException("Mã giao dịch vnp_TxnRef không hợp lệ!", HttpStatus.BAD_REQUEST);
        }

        Long bookingId = Long.parseLong(txnRef.split("_")[0]);
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionNo = fields.getOrDefault("vnp_TransactionNo", UUID.randomUUID().toString());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé #" + bookingId, HttpStatus.NOT_FOUND));

        // Avoid duplicate processing if already PAID
        if ("PAID".equals(booking.getStatus())) {
            return paymentRepository.findByBookingId(bookingId)
                    .orElseGet(() -> paymentRepository.save(Payment.builder()
                            .booking(booking)
                            .amount(booking.getTotalPrice())
                            .paymentMethod("VNPAY")
                            .status("SUCCESS")
                            .transactionId(transactionNo)
                            .build()));
        }

        boolean isSuccess = "00".equals(responseCode);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .paymentMethod("VNPAY")
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .transactionId(transactionNo)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (isSuccess) {
            booking.markAsPaid();
            List<String> ticketCodes = new ArrayList<>();
            List<String> seatNames = new ArrayList<>();

            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.issue();
                    if (ticket.getTicketCode() == null || ticket.getTicketCode().isBlank()) {
                        ticket.setTicketCode("VX" + System.currentTimeMillis() + ticket.getId());
                    }
                    // Generate QR code for ticket
                    String qrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                            ticket.getTicketCode(),
                            booking.getId(),
                            ticket.getSeat().getSeatName(),
                            ticket.getPrice().toString());
                    ticket.setQrCode(QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300));
                    
                    ticketCodes.add(ticket.getTicketCode());
                    seatNames.add(ticket.getSeat().getSeatName());
                }
                ticketRepository.saveAll(booking.getTickets());
            }

            bookingRepository.save(booking);
            seatHoldRedisService.releaseHoldByBookingId(bookingId);

            // Send confirmation email
            try {
                String departure = booking.getTrip().getRoute().getDepartureLocation();
                String arrival = booking.getTrip().getRoute().getArrivalLocation();
                String deptTime = booking.getTrip().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
                String plate = booking.getTrip().getVehicle().getLicensePlate();
                String type = booking.getTrip().getVehicle().getType();
                String seatsFormatted = String.join(", ", seatNames);
                String priceFormatted = NumberFormat.getInstance(new Locale("vi", "VN")).format(booking.getTotalPrice());

                emailService.sendBookingConfirmationEmail(
                        booking.getCustomer().getEmail(),
                        booking.getCustomer().getFullName(),
                        booking.getId(),
                        departure,
                        arrival,
                        deptTime,
                        plate,
                        type,
                        seatsFormatted,
                        priceFormatted,
                        ticketCodes
                );
            } catch (Exception e) {
                log.error("Lỗi khi gửi email xác nhận vé điện tử: {}", e.getMessage());
            }

        } else {
            // If payment failed / cancelled by user, cancel booking and release seats
            booking.cancelBooking();
            seatHoldRedisService.releaseHoldByBookingId(bookingId);
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.cancel();
                }
                ticketRepository.saveAll(booking.getTickets());
            }
            bookingRepository.save(booking);
        }

        return savedPayment;
    }

    @Override
    @Transactional
    public Payment processPaymentWebhook(Long bookingId, String transactionId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Booking is not pending payment", HttpStatus.BAD_REQUEST);
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .paymentMethod("VNPAY")
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .transactionId(transactionId)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (isSuccess) {
            booking.markAsPaid();
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.issue();
                }
                ticketRepository.saveAll(booking.getTickets());
            }
            bookingRepository.save(booking);
            seatHoldRedisService.releaseHoldByBookingId(bookingId);
        } else {
            booking.cancelBooking();
            seatHoldRedisService.releaseHoldByBookingId(bookingId);
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.release();
                }
                ticketRepository.saveAll(booking.getTickets());
            }
            bookingRepository.save(booking);
        }

        return savedPayment;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new CustomException("Không tìm thấy thông tin thanh toán cho đơn hàng #" + bookingId, HttpStatus.NOT_FOUND));
    }
}
