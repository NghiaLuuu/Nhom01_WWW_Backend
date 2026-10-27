package vn.edu.iuh.nhom01_www.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.dto.CounterSaleRequest;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.util.QRCodeGenerator;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRedisService seatHoldRedisService;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;

    @Override
    @Transactional
    public synchronized Booking createBooking(UUID customerId, Long tripId, List<Long> seatIds) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException("Không tìm thấy thông tin khách hàng!", HttpStatus.NOT_FOUND));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe!", HttpStatus.NOT_FOUND));
        tripBookingAvailabilityService.ensureWebBookingOpen(trip);

        if (seatIds == null || seatIds.isEmpty()) {
            throw new CustomException("Vui lòng chọn ít nhất một ghế!", HttpStatus.BAD_REQUEST);
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new CustomException("Một hoặc nhiều ghế không tồn tại!", HttpStatus.BAD_REQUEST);
        }

        // Validate: Ensure seats belong to the trip's vehicle
        for (Seat seat : seats) {
            if (!seat.getVehicle().getId().equals(trip.getVehicle().getId())) {
                throw new CustomException("Ghế " + seat.getSeatName() + " không thuộc phương tiện của chuyến này!", HttpStatus.BAD_REQUEST);
            }
        }

        // 1. Kiểm tra ghế ĐÃ BÁN trong DB (ISSUED, RESERVED, USED)
        List<Ticket> bookedTickets = ticketRepository.findBookedTicketsByTripIdAndSeatIds(tripId, seatIds);
        if (!bookedTickets.isEmpty()) {
            List<String> bookedSeatNames = bookedTickets.stream()
                    .map(t -> t.getSeat().getSeatName())
                    .distinct()
                    .toList();
            throw new CustomException("Ghế (" + String.join(", ", bookedSeatNames) + ") đã có người đặt mua!", HttpStatus.CONFLICT);
        }

        BigDecimal totalPrice = trip.getPrice().multiply(new BigDecimal(seats.size()));

        Booking booking = Booking.builder()
                .customer(customer)
                .trip(trip)
                .status("PENDING_PAYMENT")
                .totalPrice(totalPrice)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 2. GIỮ CHỖ TRÊN REDIS (Thời hạn 5 phút = 300 giây)
        List<String> failedHeldSeats = seatHoldRedisService.tryHoldSeats(tripId, seats, savedBooking.getId(), customerId, 300);
        if (!failedHeldSeats.isEmpty()) {
            throw new CustomException("Ghế (" + String.join(", ", failedHeldSeats) + ") đang trong thời gian giữ chỗ thanh toán của người khác!", HttpStatus.CONFLICT);
        }

        List<Ticket> tickets = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        int counter = 1;
        for (Seat seat : seats) {
            String ticketCode = "VX" + timestamp + counter;
            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(savedBooking)
                    .seat(seat)
                    .status("PENDING")
                    .price(trip.getPrice())
                    .build();
            tickets.add(ticket);
            counter++;
        }
        
        ticketRepository.saveAll(tickets);
        savedBooking.setTickets(tickets);

        return savedBooking;
    }

    @Override
    @Transactional
    public Booking createCounterSaleBooking(CounterSaleRequest request, UUID staffId) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe!", HttpStatus.NOT_FOUND));
        tripBookingAvailabilityService.ensurePosBookingOpen(trip);

        List<Long> seatIds = request.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new CustomException("Vui lòng chọn ít nhất một ghế!", HttpStatus.BAD_REQUEST);
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new CustomException("Một hoặc nhiều ghế không tồn tại!", HttpStatus.BAD_REQUEST);
        }

        for (Seat seat : seats) {
            if (!seat.getVehicle().getId().equals(trip.getVehicle().getId())) {
                throw new CustomException("Ghế " + seat.getSeatName() + " không thuộc phương tiện của chuyến này!", HttpStatus.BAD_REQUEST);
            }
        }

        // Kiểm tra ghế đã bán
        List<Ticket> bookedTickets = ticketRepository.findBookedTicketsByTripIdAndSeatIds(request.getTripId(), seatIds);
        if (!bookedTickets.isEmpty()) {
            List<String> bookedNames = bookedTickets.stream().map(t -> t.getSeat().getSeatName()).distinct().toList();
            throw new CustomException("Ghế (" + String.join(", ", bookedNames) + ") đã có người mua!", HttpStatus.CONFLICT);
        }

        // Tìm hoặc tạo tài khoản khách hàng mua tại quầy
        Customer customer = customerRepository.findByPhoneNumber(request.getCustomerPhone())
                .orElseGet(() -> {
                    String email = request.getCustomerEmail();
                    if (email == null || email.isBlank()) {
                        email = "pos_" + request.getCustomerPhone().replaceAll("[^0-9]", "") + "@vexe.vn";
                    }
                    Customer existingByEmail = customerRepository.findByEmail(email);
                    if (existingByEmail != null) {
                        return existingByEmail;
                    }
                    Customer newCust = Customer.builder()
                            .fullName(request.getCustomerName())
                            .phoneNumber(request.getCustomerPhone())
                            .email(email)
                            .password(passwordEncoder.encode("pos_" + UUID.randomUUID().toString().substring(0, 8)))
                            .status("ACTIVE")
                            .build();
                    return customerRepository.save(newCust);
                });

        // A counter sale identifies the passenger by the name entered by staff.
        // When the phone number belongs to a seeded/previous placeholder account,
        // preserve the real passenger name so admin ticket lists do not show
        // labels such as "Khách hàng 12" instead of the customer's name.
        String customerName = request.getCustomerName() == null ? "" : request.getCustomerName().trim();
        if (!customerName.isBlank() && !customerName.equals(customer.getFullName())) {
            customer.setFullName(customerName);
            customer = customerRepository.save(customer);
        }

        BigDecimal totalPrice = trip.getPrice().multiply(new BigDecimal(seats.size()));

        Booking booking = Booking.builder()
                .customer(customer)
                .trip(trip)
                .status("PAID")
                .totalPrice(totalPrice)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        int counter = 1;
        for (Seat seat : seats) {
            String ticketCode = "VXPOS" + timestamp + counter;
            String qrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                    ticketCode,
                    savedBooking.getId(),
                    seat.getSeatName(),
                    trip.getPrice().toString());
            String qrBase64 = QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300);

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(savedBooking)
                    .seat(seat)
                    .status("ISSUED")
                    .price(trip.getPrice())
                    .qrCode(qrBase64)
                    .build();
            tickets.add(ticket);
            counter++;
        }

        ticketRepository.saveAll(tickets);
        savedBooking.setTickets(tickets);

        // Tạo bản ghi thanh toán POS
        String payMethod = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? request.getPaymentMethod().toUpperCase()
                : "CASH";
        Payment payment = Payment.builder()
                .booking(savedBooking)
                .amount(totalPrice)
                .paymentMethod(payMethod)
                .status("SUCCESS")
                .transactionId("POS_" + savedBooking.getId() + "_" + System.currentTimeMillis())
                .build();
        paymentRepository.save(payment);

        return savedBooking;
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void releaseExpiredBookings() {
        // Redis TTL is the sole source of truth for a pending booking's hold time.
        // Once its booking_hold key expires, the booking can no longer be paid for.
        List<Booking> expiredBookings = bookingRepository.findByStatus("PENDING_PAYMENT").stream()
                .filter(booking -> !seatHoldRedisService.isBookingHoldActive(booking.getId()))
                .toList();
        
        for (Booking booking : expiredBookings) {
            booking.cancelBooking();
            seatHoldRedisService.releaseHoldByBookingId(booking.getId());
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.cancel();
                }
            }
        }
        
        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
            log.info("Đã tự động chuyển {} đơn đặt vé hết hạn giữ chỗ sang trạng thái CANCELLED (Đã hủy).", expiredBookings.size());
        }
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getMyBookingHistory(UUID customerId){
        return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long bookingId, UUID customerId) {
        if (customerId == null) {
            return bookingRepository.findById(bookingId).orElseThrow(() ->
                    new CustomException("Không tìm thấy đơn đặt vé!", HttpStatus.NOT_FOUND));
        }
        return bookingRepository.findByIdAndCustomerId(bookingId, customerId).orElseThrow(() ->
                new CustomException("Không tìm thấy đơn đặt vé!", HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchBookings(String phoneNumber, String ticketCode, UUID customerId) {
        if (ticketCode != null && !ticketCode.trim().isEmpty()) {
            return bookingRepository.findDistinctByTickets_TicketCode(ticketCode.trim())
                    .map(List::of)
                    .orElse(List.of());
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            return bookingRepository.findByCustomer_PhoneNumberOrderByCreatedAtDesc(phoneNumber.trim());
        }
        if (customerId != null) {
            return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        }
        return bookingRepository.findAll();
    }

    @Override
    @Transactional
    public Booking cancelPendingBooking(Long bookingId, UUID customerId) {
        Booking booking = (customerId == null)
                ? bookingRepository.findById(bookingId).orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé!", HttpStatus.NOT_FOUND))
                : bookingRepository.findByIdAndCustomerId(bookingId, customerId).orElseThrow(() -> new CustomException("Không tìm thấy đơn đặt vé!", HttpStatus.NOT_FOUND));

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Chỉ có thể hủy đơn đặt vé đang chờ thanh toán!", HttpStatus.BAD_REQUEST);
        }

        booking.cancelBooking();
        seatHoldRedisService.releaseHoldByBookingId(bookingId);
        if (booking.getTickets() != null) {
            for (Ticket ticket : booking.getTickets()) {
                ticket.release();
            }
        }

        return bookingRepository.save(booking);
    }
}
