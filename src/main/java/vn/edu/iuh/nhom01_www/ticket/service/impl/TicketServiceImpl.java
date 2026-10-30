package vn.edu.iuh.nhom01_www.ticket.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.common.util.QRCodeGenerator;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.dto.*;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.ticket.service.TicketService;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final SeatHoldRedisService seatHoldRedisService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    @Value("${ticket.checkin.open-minutes-before:60}")
    private int checkInOpenMinutesBefore;

    @Value("${ticket.checkin.close-minutes-before:10}")
    private int checkInCloseMinutesBefore;

    @Value("${ticket.noshow.reschedule-fee-percent:20}")
    private int rescheduleFeePercent;

    @Value("${ticket.noshow.min-reschedule-fee:30000}")
    private BigDecimal minRescheduleFee;

    @Override
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new CustomException("Không tìm thấy vé!", HttpStatus.NOT_FOUND));
    }

    @Override
    public Ticket getTicketByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new CustomException("Không tìm thấy vé với mã: " + ticketCode, HttpStatus.NOT_FOUND));
    }

    @Override
    public List<Ticket> getTicketsByBookingId(Long bookingId) {
        return ticketRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Long> getOccupiedSeatIds(Long tripId) {
        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByTripId(tripId);
        List<Long> heldSeatIds = seatHoldRedisService.getHeldSeatIdsByTripId(tripId);
        Set<Long> allOccupied = new LinkedHashSet<>(bookedSeatIds);
        allOccupied.addAll(heldSeatIds);
        return new ArrayList<>(allOccupied);
    }

    @Override
    @Transactional
    public Ticket updateTicketStatus(Long id, String status) {
        Ticket ticket = getTicketById(id);
        if ("USED".equalsIgnoreCase(status)) {
            ticket.use();
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            ticket.cancel();
        } else if ("ISSUED".equalsIgnoreCase(status)) {
            ticket.issue();
        } else if ("RELEASED".equalsIgnoreCase(status)) {
            ticket.release();
        } else if ("NO_SHOW".equalsIgnoreCase(status)) {
            ticket.markNoShow();
        } else if ("RESCHEDULED".equalsIgnoreCase(status)) {
            ticket.markRescheduled();
        } else {
            throw new CustomException("Trạng thái không hợp lệ: " + status, HttpStatus.BAD_REQUEST);
        }
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public String generateQrCodeForTicket(Long id) {
        Ticket ticket = getTicketById(id);
        if (ticket.getQrCode() != null && !ticket.getQrCode().isBlank()) {
            return ticket.getQrCode();
        }

        String qrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                ticket.getTicketCode(),
                ticket.getBooking().getId(),
                ticket.getSeat().getSeatName(),
                ticket.getPrice().toString());

        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300);
        ticket.setQrCode(qrBase64);
        ticketRepository.save(ticket);
        return qrBase64;
    }

    @Override
    @Transactional
    public CheckInResponse checkInTicket(CheckInRequest request) {
        String rawInput = request.getTicketCodeOrQr();
        if (rawInput == null || rawInput.isBlank()) {
            throw new CustomException("Mã vé hoặc chuỗi QR không được để trống!", HttpStatus.BAD_REQUEST);
        }

        String parsedCode = rawInput.trim();
        // Parse trường hợp quét toàn bộ nội dung mã QR: TICKET:VX123...|BOOKING:...
        if (parsedCode.startsWith("TICKET:")) {
            int pipeIndex = parsedCode.indexOf('|');
            if (pipeIndex > 7) {
                parsedCode = parsedCode.substring(7, pipeIndex);
            } else {
                parsedCode = parsedCode.substring(7);
            }
        }

        final String finalTicketCode = parsedCode;
        Ticket ticket = ticketRepository.findByTicketCode(finalTicketCode)
                .orElseThrow(() -> new CustomException("Không tìm thấy vé với mã: " + finalTicketCode, HttpStatus.NOT_FOUND));

        var trip = ticket.getBooking().getTrip();

        // 1. Kiểm tra trạng thái chuyến xe
        if (trip != null) {
            if ("CANCELLED".equalsIgnoreCase(trip.getStatus())) {
                throw new CustomException("Chuyến xe này đã bị HỦY, không thể thực hiện soát vé!", HttpStatus.BAD_REQUEST);
            }
            if ("COMPLETED".equalsIgnoreCase(trip.getStatus())) {
                throw new CustomException("Chuyến xe này đã HOÀN THÀNH hành trình!", HttpStatus.BAD_REQUEST);
            }
        }

        // 2. Kiểm tra vé có thuộc đúng chuyến xe này không (nếu có truyền tripId)
        if (request.getTripId() != null && trip != null) {
            Long actualTripId = trip.getId();
            if (!actualTripId.equals(request.getTripId())) {
                throw new CustomException("Vé này không thuộc chuyến xe #" + request.getTripId() 
                        + " mà thuộc chuyến xe #" + actualTripId + "!", HttpStatus.BAD_REQUEST);
            }
        }

        // 3. Kiểm tra trạng thái vé
        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            String usedTimeStr = ticket.getUsedAt() != null
                    ? ticket.getUsedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))
                    : "trước đó";
            throw new CustomException("CẢNH BÁO: Vé này đã được check-in/soát vé vào lúc " + usedTimeStr + "!", HttpStatus.CONFLICT);
        }

        if ("CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé này đã bị HỦY, không thể sử dụng để lên xe!", HttpStatus.BAD_REQUEST);
        }

        if ("RESCHEDULED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé này đã được ĐỔI sang chuyến xe khác, không còn hiệu lực trên chuyến này!", HttpStatus.BAD_REQUEST);
        }

        if ("RELEASED".equalsIgnoreCase(ticket.getStatus()) || "PENDING".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé chưa hoàn tất thanh toán hoặc đã bị giải phóng chỗ!", HttpStatus.BAD_REQUEST);
        }

        boolean isForce = Boolean.TRUE.equals(request.getForceCheckIn()) && currentUserService.isStaffOrAdmin();

        // Xử lý vé đã bị đánh dấu NO_SHOW từ trước
        if ("NO_SHOW".equalsIgnoreCase(ticket.getStatus())) {
            if (isForce) {
                // Nhân viên/Phụ xe linh động cho khách vắng mặt lên xe
                ticket.use();
                ticketRepository.save(ticket);
                return buildCheckInResponse(ticket, trip, "Soát vé cưỡng chế thành công cho vé vắng mặt (No-Show)!");
            }
            throw new CustomException(
                    "Vé này đã bị chuyển sang trạng thái VẮNG MẶT (NO-SHOW) do hành khách không có mặt trước giờ chốt vé! "
                    + "Quý khách vui lòng liên hệ quầy vé để được hỗ trợ hủy hoặc đổi sang chuyến tiếp theo.",
                    HttpStatus.BAD_REQUEST);
        }

        if (!"ISSUED".equalsIgnoreCase(ticket.getStatus()) && !"RESERVED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Trạng thái vé không hợp lệ để lên xe: " + ticket.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // 4. RÀNG BUỘC KHUNG GIỜ SOÁT VÉ (CHECK-IN TIME WINDOW)
        if (trip != null && trip.getDepartureTime() != null && !isForce) {
            LocalDateTime departureTime = trip.getDepartureTime();
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime checkInOpenTime = departureTime.minusMinutes(checkInOpenMinutesBefore);
            LocalDateTime checkInCloseTime = departureTime.minusMinutes(checkInCloseMinutesBefore);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

            if (now.isBefore(checkInOpenTime)) {
                throw new CustomException(
                        String.format("Chưa đến giờ soát vé cho chuyến xe này! Cổng soát vé sẽ mở lúc %s (tối thiểu %d phút trước giờ xuất bến lúc %s).",
                                checkInOpenTime.format(dtf),
                                checkInOpenMinutesBefore,
                                departureTime.format(dtf)),
                        HttpStatus.BAD_REQUEST);
            }

            if (now.isAfter(checkInCloseTime)) {
                // TỰ ĐỘNG CHUYỂN TRẠNG THÁI VÉ SANG NO_SHOW VÌ ĐẾN TRỄ SAU GIỜ CHỐT VÉ
                ticket.markNoShow();
                ticketRepository.save(ticket);

                if (now.isBefore(departureTime)) {
                    throw new CustomException(
                            String.format("Cổng soát vé đã ĐÓNG lúc %s (đóng trước giờ xe xuất bến %d phút để phụ xe chốt danh sách và xếp hành lý)! "
                                    + "Vé đã được chuyển sang trạng thái Vắng mặt (NO-SHOW). Xe chuẩn bị khởi hành lúc %s. Quý khách vui lòng liên hệ quầy vé để được hỗ trợ.",
                                    checkInCloseTime.format(dtf),
                                    checkInCloseMinutesBefore,
                                    departureTime.format(dtf)),
                            HttpStatus.BAD_REQUEST);
                } else {
                    throw new CustomException(
                            String.format("Chuyến xe đã khởi hành vào lúc %s! "
                                    + "Vé đã được chuyển sang trạng thái Vắng mặt (NO-SHOW). Quý khách vui lòng liên hệ quầy vé để hủy hoặc đổi chuyến tiếp theo.",
                                    departureTime.format(dtf)),
                            HttpStatus.BAD_REQUEST);
                }
            }
        }

        // Đánh dấu vé đã sử dụng (USED)
        ticket.use();
        ticketRepository.save(ticket);

        return buildCheckInResponse(ticket, trip, "Soát vé thành công! Chúc hành khách có chuyến đi vui vẻ.");
    }

    @Override
    @Transactional
    public VerifyTicketResponse verifyTicket(VerifyTicketRequest request) {
        String normalizedCode = normalizeVerificationCode(request != null ? request.getCode() : null);
        if (normalizedCode.isBlank()) {
            return invalidVerification("INVALID_CODE", null);
        }

        Ticket ticket = ticketRepository.findByTicketCode(normalizedCode).orElse(null);
        if (ticket == null) {
            return invalidVerification("TICKET_NOT_FOUND", null);
        }

        Booking booking = ticket.getBooking();
        if (booking == null || !"PAID".equalsIgnoreCase(booking.getStatus())) {
            return invalidVerification("NOT_PAID", ticket);
        }

        Trip trip = booking.getTrip();
        if (trip == null || trip.getDepartureTime() == null
                || !trip.getDepartureTime().toLocalDate().equals(LocalDate.now())) {
            return invalidVerification("WRONG_DEPARTURE_DATE", ticket);
        }

        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            return invalidVerification("ALREADY_USED", ticket);
        }

        if (!"UNUSED".equalsIgnoreCase(ticket.getStatus())
                && !"ISSUED".equalsIgnoreCase(ticket.getStatus())) {
            return invalidVerification("TICKET_NOT_UNUSED", ticket);
        }

        VerifyTicketResponse.TicketDetails ticketDetails = buildVerificationTicketDetails(ticket);

        LocalDateTime usedAt = LocalDateTime.now();
        int updatedRows = ticketRepository.markUsedIfUnused(
                ticket.getId(), usedAt, currentUserService.getCurrentStaff());
        if (updatedRows != 1) {
            return VerifyTicketResponse.builder()
                    .valid(false)
                    .reason("ALREADY_USED")
                    .ticket(ticketDetails)
                    .build();
        }

        return VerifyTicketResponse.builder()
                .valid(true)
                .reason("VERIFIED")
                .ticket(ticketDetails)
                .build();
    }

    private String normalizeVerificationCode(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        String code = rawCode.trim();
        // QR chính thức của vé có dạng: TICKET:<mã vé>|BOOKING:<id>|SEAT:<ghế>|PRICE:<giá>
        if (code.regionMatches(true, 0, "TICKET:", 0, 7)) {
            int pipeIndex = code.indexOf('|');
            code = pipeIndex > 7 ? code.substring(7, pipeIndex) : code.substring(7);
        }
        return code.replaceAll("[*\\s]+", "");
    }

    private VerifyTicketResponse invalidVerification(String reason, Ticket ticket) {
        return VerifyTicketResponse.builder()
                .valid(false)
                .reason(reason)
                .ticket(ticket == null ? null : buildVerificationTicketDetails(ticket))
                .build();
    }

    private VerifyTicketResponse.TicketDetails buildVerificationTicketDetails(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = booking != null ? booking.getTrip() : null;
        var customer = booking != null ? booking.getCustomer() : null;
        var seat = ticket.getSeat();

        String route = null;
        if (trip != null && trip.getRoute() != null) {
            String departure = trip.getRoute().getDepartureLocation();
            String arrival = trip.getRoute().getArrivalLocation();
            if (departure != null && arrival != null) {
                route = departure + " - " + arrival;
            } else if (departure != null) {
                route = departure;
            } else {
                route = arrival;
            }
        }

        return VerifyTicketResponse.TicketDetails.builder()
                .code(ticket.getTicketCode())
                .passengerName(customer != null ? customer.getFullName() : null)
                .seat(seat != null ? seat.getSeatName() : null)
                .route(route)
                .departureTime(trip != null ? trip.getDepartureTime() : null)
                .build();
    }

    private CheckInResponse buildCheckInResponse(Ticket ticket, Trip trip, String message) {
        var customer = ticket.getBooking().getCustomer();
        var seat = ticket.getSeat();

        return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getStatus())
                .seatName(seat != null ? seat.getSeatName() : null)
                .floor(seat != null ? seat.getFloor() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerPhone(customer != null ? customer.getPhoneNumber() : null)
                .bookingId(ticket.getBooking().getId())
                .tripId(trip != null ? trip.getId() : null)
                .departureLocation(trip != null && trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : null)
                .arrivalLocation(trip != null && trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : null)
                .departureTime(trip != null ? trip.getDepartureTime() : null)
                .licensePlate(trip != null && trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : null)
                .vehicleType(trip != null && trip.getVehicle() != null ? trip.getVehicle().getType() : null)
                .price(ticket.getPrice())
                .usedAt(ticket.getUsedAt())
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerManifestDto> getPassengerManifest(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new CustomException("Không tìm thấy chuyến xe #" + tripId, HttpStatus.NOT_FOUND);
        }

        List<Ticket> tickets = ticketRepository.findPassengerManifestByTripId(
                tripId, List.of("ISSUED", "USED", "CANCELLED", "NO_SHOW", "RESCHEDULED"));

        return tickets.stream().map(t -> PassengerManifestDto.builder()
                .ticketId(t.getId())
                .ticketCode(t.getTicketCode())
                .status(t.getStatus())
                .seatId(t.getSeat() != null ? t.getSeat().getId() : null)
                .seatName(t.getSeat() != null ? t.getSeat().getSeatName() : null)
                .floor(t.getSeat() != null ? t.getSeat().getFloor() : null)
                .customerName(t.getBooking() != null && t.getBooking().getCustomer() != null ? t.getBooking().getCustomer().getFullName() : null)
                .customerPhone(t.getBooking() != null && t.getBooking().getCustomer() != null ? t.getBooking().getCustomer().getPhoneNumber() : null)
                .bookingId(t.getBooking() != null ? t.getBooking().getId() : null)
                .price(t.getPrice())
                .usedAt(t.getUsedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Ticket changeSeat(Long ticketId, Long newSeatId) {
        Ticket ticket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(ticket);

        if (!"ISSUED".equalsIgnoreCase(ticket.getStatus()) && !"RESERVED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Chỉ có thể đổi chỗ đối với vé đã phát hành hợp lệ!", HttpStatus.BAD_REQUEST);
        }

        Long tripId = ticket.getBooking().getTrip().getId();
        Seat newSeat = seatRepository.findById(newSeatId)
                .orElseThrow(() -> new CustomException("Không tìm thấy ghế mới!", HttpStatus.NOT_FOUND));

        Long vehicleId = ticket.getBooking().getTrip().getVehicle().getId();
        if (!newSeat.getVehicle().getId().equals(vehicleId)) {
            throw new CustomException("Ghế mới không thuộc phương tiện của chuyến xe này!", HttpStatus.BAD_REQUEST);
        }

        if (newSeat.getId().equals(ticket.getSeat().getId())) {
            throw new CustomException("Ghế mới trùng với ghế hiện tại!", HttpStatus.BAD_REQUEST);
        }

        List<Ticket> booked = ticketRepository.findBookedTicketsByTripIdAndSeatIds(tripId, List.of(newSeatId));
        if (!booked.isEmpty()) {
            throw new CustomException("Ghế " + newSeat.getSeatName() + " đã có người mua!", HttpStatus.CONFLICT);
        }

        List<Long> heldSeatIds = seatHoldRedisService.getHeldSeatIdsByTripId(tripId);
        if (heldSeatIds.contains(newSeatId)) {
            throw new CustomException("Ghế " + newSeat.getSeatName() + " đang trong thời gian giữ chỗ của người khác!", HttpStatus.CONFLICT);
        }

        ticket.setSeat(newSeat);

        String qrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                ticket.getTicketCode(),
                ticket.getBooking().getId(),
                newSeat.getSeatName(),
                ticket.getPrice().toString());
        ticket.setQrCode(QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300));

        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public Ticket cancelSingleTicket(Long ticketId, String reason) {
        Ticket ticket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(ticket);

        if ("CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé này đã được hủy trước đó!", HttpStatus.BAD_REQUEST);
        }

        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé đã sử dụng để lên xe, không thể hủy!", HttpStatus.BAD_REQUEST);
        }

        if (!"ISSUED".equalsIgnoreCase(ticket.getStatus()) && !"RESERVED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Chỉ có thể hủy vé đã thanh toán phát hành!", HttpStatus.BAD_REQUEST);
        }

        ticket.cancel();
        ticketRepository.save(ticket);

        Booking booking = ticket.getBooking();
        if (booking != null) {
            List<Ticket> bookingTickets = ticketRepository.findByBookingId(booking.getId());
            boolean allCancelled = bookingTickets.stream().allMatch(t -> "CANCELLED".equalsIgnoreCase(t.getStatus()));
            if (allCancelled) {
                booking.cancelBooking();
                bookingRepository.save(booking);
            }
        }

        return ticket;
    }

    // ==================== CÁC CHỨC NĂNG XỬ LÝ NO-SHOW ====================

    @Override
    @Transactional
    public Ticket markTicketAsNoShow(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(ticket);

        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé đã được soát sử dụng lên xe, không thể đánh dấu No-Show!", HttpStatus.BAD_REQUEST);
        }
        if ("CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé đã bị hủy trước đó!", HttpStatus.BAD_REQUEST);
        }

        ticket.markNoShow();
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public NoShowSummaryDto markTripNoShows(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new CustomException("Không tìm thấy chuyến xe #" + tripId, HttpStatus.NOT_FOUND);
        }

        List<Ticket> unboardedTickets = ticketRepository.findIssuedTicketsByTripId(tripId);
        for (Ticket ticket : unboardedTickets) {
            ticket.markNoShow();
        }
        ticketRepository.saveAll(unboardedTickets);

        List<String> ticketCodes = unboardedTickets.stream().map(Ticket::getTicketCode).toList();
        return NoShowSummaryDto.builder()
                .tripId(tripId)
                .totalNoShowMarked(unboardedTickets.size())
                .noShowTicketCodes(ticketCodes)
                .message(String.format("Đã chốt danh sách xuất bến chuyến xe #%d: Chuyển %d vé chưa lên xe sang trạng thái Vắng mặt (NO-SHOW).",
                        tripId, unboardedTickets.size()))
                .build();
    }

    @Override
    @Transactional
    public int autoMarkPastDepartureNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(checkInCloseMinutesBefore);
        List<Ticket> expiredTickets = ticketRepository.findIssuedTicketsPastGateClosure(cutoff);
        if (expiredTickets.isEmpty()) {
            return 0;
        }
        for (Ticket ticket : expiredTickets) {
            ticket.markNoShow();
        }
        ticketRepository.saveAll(expiredTickets);
        return expiredTickets.size();
    }

    @Override
    @Transactional
    public Ticket cancelNoShowTicket(Long ticketId, String reason) {
        Ticket ticket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(ticket);

        if ("CANCELLED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé này đã được hủy trước đó!", HttpStatus.BAD_REQUEST);
        }
        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            throw new CustomException("Vé đã sử dụng, không thể hủy!", HttpStatus.BAD_REQUEST);
        }

        boolean isNoShow = "NO_SHOW".equalsIgnoreCase(ticket.getStatus()) || isPastGateClosure(ticket);
        if (!isNoShow) {
            // Nếu chưa qua giờ chốt vé, chuyển về quy trình hủy vé thông thường
            return cancelSingleTicket(ticketId, reason);
        }

        ticket.cancel();
        ticketRepository.save(ticket);

        Booking booking = ticket.getBooking();
        if (booking != null) {
            List<Ticket> bookingTickets = ticketRepository.findByBookingId(booking.getId());
            boolean allCancelled = bookingTickets.stream().allMatch(t -> "CANCELLED".equalsIgnoreCase(t.getStatus()));
            if (allCancelled) {
                booking.cancelBooking();
                bookingRepository.save(booking);
            }
        }

        log.info("Đã hủy hoàn toàn vé No-Show #{} (Không hoàn tiền theo chính sách vắng mặt). Lý do: {}",
                ticket.getTicketCode(), reason != null ? reason : "Hành khách vắng mặt");
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleOptionDto> getRescheduleOptions(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(ticket);

        Trip oldTrip = ticket.getBooking().getTrip();
        if (oldTrip == null || oldTrip.getRoute() == null) {
            throw new CustomException("Không xác định được tuyến đường của vé này!", HttpStatus.BAD_REQUEST);
        }

        Long routeId = oldTrip.getRoute().getId();
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(checkInCloseMinutesBefore);
        List<Trip> upcomingTrips = tripRepository.findAvailableNextTripsByRouteId(routeId, cutoff);

        BigDecimal originalPrice = ticket.getPrice();
        List<RescheduleOptionDto> options = new ArrayList<>();

        for (Trip trip : upcomingTrips) {
            if (trip.getId().equals(oldTrip.getId())) {
                continue; // Bỏ qua chuyến cũ
            }

            List<Seat> vehicleSeats = seatRepository.findByVehicleId(trip.getVehicle().getId());
            List<Long> occupiedSeatIds = getOccupiedSeatIds(trip.getId());

            List<Long> availableSeatIds = vehicleSeats.stream()
                    .map(Seat::getId)
                    .filter(sid -> !occupiedSeatIds.contains(sid))
                    .toList();

            if (availableSeatIds.isEmpty()) {
                continue; // Hết chỗ trên chuyến này
            }

            BigDecimal rescheduleFee = calculateRescheduleFee(originalPrice);
            BigDecimal newPrice = trip.getPrice();
            BigDecimal priceDiff = newPrice.compareTo(originalPrice) > 0 ? newPrice.subtract(originalPrice) : BigDecimal.ZERO;
            BigDecimal totalAdditionalFee = rescheduleFee.add(priceDiff);

            options.add(RescheduleOptionDto.builder()
                    .tripId(trip.getId())
                    .departureTime(trip.getDepartureTime())
                    .departureLocation(trip.getRoute().getDepartureLocation())
                    .arrivalLocation(trip.getRoute().getArrivalLocation())
                    .vehicleType(trip.getVehicle().getType())
                    .licensePlate(trip.getVehicle().getLicensePlate())
                    .tripPrice(newPrice)
                    .totalSeats(vehicleSeats.size())
                    .availableSeatsCount(availableSeatIds.size())
                    .availableSeatIds(availableSeatIds)
                    .originalPrice(originalPrice)
                    .rescheduleFee(rescheduleFee)
                    .priceDifference(priceDiff)
                    .totalAdditionalFee(totalAdditionalFee)
                    .build());
        }

        return options;
    }

    @Override
    @Transactional
    public RescheduleTicketResponse rescheduleNoShowTicket(Long ticketId, RescheduleTicketRequest request) {
        Ticket oldTicket = getTicketById(ticketId);
        validateTicketOwnershipOrStaff(oldTicket);

        if ("USED".equalsIgnoreCase(oldTicket.getStatus())) {
            throw new CustomException("Vé đã sử dụng, không thể đổi chuyến!", HttpStatus.BAD_REQUEST);
        }
        if ("CANCELLED".equalsIgnoreCase(oldTicket.getStatus())) {
            throw new CustomException("Vé đã bị hủy trước đó, không thể đổi chuyến!", HttpStatus.BAD_REQUEST);
        }
        if ("RESCHEDULED".equalsIgnoreCase(oldTicket.getStatus())) {
            throw new CustomException("Vé này đã được đổi sang chuyến khác trước đó!", HttpStatus.BAD_REQUEST);
        }

        Trip oldTrip = oldTicket.getBooking().getTrip();
        Long oldRouteId = oldTrip.getRoute().getId();
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(checkInCloseMinutesBefore);

        // 1. Xác định chuyến xe mới
        Trip newTrip;
        if (request != null && request.getNewTripId() != null) {
            newTrip = tripRepository.findById(request.getNewTripId())
                    .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe mới #" + request.getNewTripId(), HttpStatus.NOT_FOUND));
        } else {
            // Tự động tìm chuyến xe tiếp theo sớm nhất của cùng tuyến
            List<Trip> upcomingTrips = tripRepository.findAvailableNextTripsByRouteId(oldRouteId, cutoff);
            newTrip = upcomingTrips.stream()
                    .filter(t -> !t.getId().equals(oldTrip.getId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe tiếp theo nào phù hợp cho tuyến đường này!", HttpStatus.BAD_REQUEST));
        }

        if (newTrip.getId().equals(oldTrip.getId())) {
            throw new CustomException("Chuyến xe mới phải khác với chuyến xe cũ bị trễ!", HttpStatus.BAD_REQUEST);
        }
        if ("CANCELLED".equalsIgnoreCase(newTrip.getStatus()) || "COMPLETED".equalsIgnoreCase(newTrip.getStatus())) {
            throw new CustomException("Chuyến xe mới không khả dụng (đã hủy hoặc hoàn thành)!", HttpStatus.BAD_REQUEST);
        }
        if (newTrip.getDepartureTime().isBefore(cutoff)) {
            throw new CustomException("Chuyến xe mới đã qua giờ chốt vé xuất bến!", HttpStatus.BAD_REQUEST);
        }

        // 2. Xác định ghế mới trên chuyến mới
        List<Seat> vehicleSeats = seatRepository.findByVehicleId(newTrip.getVehicle().getId());
        List<Long> occupiedSeatIds = getOccupiedSeatIds(newTrip.getId());

        Seat newSeat;
        if (request != null && request.getNewSeatId() != null) {
            newSeat = seatRepository.findById(request.getNewSeatId())
                    .orElseThrow(() -> new CustomException("Không tìm thấy ghế mới #" + request.getNewSeatId(), HttpStatus.NOT_FOUND));
            if (!newSeat.getVehicle().getId().equals(newTrip.getVehicle().getId())) {
                throw new CustomException("Ghế mới không thuộc phương tiện của chuyến xe mới!", HttpStatus.BAD_REQUEST);
            }
            if (occupiedSeatIds.contains(newSeat.getId())) {
                throw new CustomException("Ghế " + newSeat.getSeatName() + " đã có người mua hoặc đang được giữ chỗ!", HttpStatus.CONFLICT);
            }
        } else {
            // Tự động gán ghế còn trống (ưu tiên cùng tầng với ghế cũ nếu có thể)
            newSeat = vehicleSeats.stream()
                    .filter(s -> !occupiedSeatIds.contains(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Chuyến xe mới #" + newTrip.getId() + " đã hết ghế trống!", HttpStatus.BAD_REQUEST));
        }

        // 3. Tính toán chi phí
        BigDecimal originalPrice = oldTicket.getPrice();
        BigDecimal rescheduleFee = calculateRescheduleFee(originalPrice);
        BigDecimal newTripPrice = newTrip.getPrice();
        BigDecimal priceDifference = newTripPrice.compareTo(originalPrice) > 0 ? newTripPrice.subtract(originalPrice) : BigDecimal.ZERO;
        BigDecimal totalAdditionalFee = rescheduleFee.add(priceDifference);

        // 4. Cập nhật vé cũ sang trạng thái RESCHEDULED
        oldTicket.markRescheduled();
        ticketRepository.save(oldTicket);

        // 5. Tạo đơn Booking mới cho chuyến tiếp theo
        Booking newBooking = Booking.builder()
                .customer(oldTicket.getBooking().getCustomer())
                .trip(newTrip)
                .status("PAID")
                .totalPrice(newTripPrice.add(totalAdditionalFee))
                .build();
        newBooking = bookingRepository.save(newBooking);

        // 6. Phát hành vé mới
        String newTicketCode = "TK" + System.currentTimeMillis() + (int)(Math.random() * 900 + 100);
        String qrPayload = String.format("TICKET:%s|BOOKING:%d|SEAT:%s|PRICE:%s",
                newTicketCode,
                newBooking.getId(),
                newSeat.getSeatName(),
                newTripPrice.toString());
        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(qrPayload, 300, 300);

        Ticket newTicket = Ticket.builder()
                .ticketCode(newTicketCode)
                .booking(newBooking)
                .seat(newSeat)
                .price(newTripPrice)
                .status("ISSUED")
                .qrCode(qrBase64)
                .build();
        newTicket = ticketRepository.save(newTicket);

        // 7. Ghi nhận giao dịch thanh toán phụ phí đổi vé
        String payMethod = (request != null && request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod() : "CASH";
        Payment payment = Payment.builder()
                .booking(newBooking)
                .amount(totalAdditionalFee)
                .paymentMethod(payMethod)
                .status("SUCCESS")
                .transactionId("RESCHEDULE_" + System.currentTimeMillis())
                .build();
        paymentRepository.save(payment);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        String successMsg = String.format("Đổi chuyến thành công cho vé No-Show #%s sang chuyến #%d khởi hành lúc %s (Ghế %s). Phụ thu đã thanh toán: %s VNĐ.",
                oldTicket.getTicketCode(),
                newTrip.getId(),
                newTrip.getDepartureTime().format(dtf),
                newSeat.getSeatName(),
                totalAdditionalFee.toPlainString());

        return RescheduleTicketResponse.builder()
                .oldTicketId(oldTicket.getId())
                .oldTicketCode(oldTicket.getTicketCode())
                .oldTripId(oldTrip.getId())
                .oldSeatName(oldTicket.getSeat() != null ? oldTicket.getSeat().getSeatName() : null)
                .newTicketId(newTicket.getId())
                .newTicketCode(newTicket.getTicketCode())
                .newTripId(newTrip.getId())
                .newSeatName(newSeat.getSeatName())
                .newFloor(newSeat.getFloor())
                .newDepartureTime(newTrip.getDepartureTime())
                .departureLocation(newTrip.getRoute() != null ? newTrip.getRoute().getDepartureLocation() : null)
                .arrivalLocation(newTrip.getRoute() != null ? newTrip.getRoute().getArrivalLocation() : null)
                .licensePlate(newTrip.getVehicle() != null ? newTrip.getVehicle().getLicensePlate() : null)
                .vehicleType(newTrip.getVehicle() != null ? newTrip.getVehicle().getType() : null)
                .originalPrice(originalPrice)
                .newTripPrice(newTripPrice)
                .rescheduleFee(rescheduleFee)
                .priceDifference(priceDifference)
                .totalAdditionalFee(totalAdditionalFee)
                .status(newTicket.getStatus())
                .qrCode(newTicket.getQrCode())
                .message(successMsg)
                .build();
    }

    private BigDecimal calculateRescheduleFee(BigDecimal originalPrice) {
        BigDecimal fee = originalPrice.multiply(BigDecimal.valueOf(rescheduleFeePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return fee.compareTo(minRescheduleFee) < 0 ? minRescheduleFee : fee;
    }

    private boolean isPastGateClosure(Ticket ticket) {
        if (ticket.getBooking() == null || ticket.getBooking().getTrip() == null || ticket.getBooking().getTrip().getDepartureTime() == null) {
            return false;
        }
        LocalDateTime departureTime = ticket.getBooking().getTrip().getDepartureTime();
        LocalDateTime gateCloseTime = departureTime.minusMinutes(checkInCloseMinutesBefore);
        return LocalDateTime.now().isAfter(gateCloseTime);
    }

    private void validateTicketOwnershipOrStaff(Ticket ticket) {
        if (currentUserService.isStaffOrAdmin()) {
            return;
        }
        var currentCustomerOpt = currentUserService.getOptionalCurrentCustomer();
        if (currentCustomerOpt.isEmpty()) {
            throw new CustomException("Người dùng chưa đăng nhập!", HttpStatus.UNAUTHORIZED);
        }
        var customer = currentCustomerOpt.get();
        if (ticket.getBooking() == null || ticket.getBooking().getCustomer() == null
                || !ticket.getBooking().getCustomer().getId().equals(customer.getId())) {
            throw new CustomException("Bạn không có quyền thao tác trên vé này!", HttpStatus.FORBIDDEN);
        }
    }
}
