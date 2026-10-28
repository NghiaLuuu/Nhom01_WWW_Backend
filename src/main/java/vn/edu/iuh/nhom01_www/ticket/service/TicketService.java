package vn.edu.iuh.nhom01_www.ticket.service;

import vn.edu.iuh.nhom01_www.ticket.dto.*;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;

import java.util.List;

public interface TicketService {
    Ticket getTicketById(Long id);
    Ticket getTicketByCode(String ticketCode);
    List<Ticket> getTicketsByBookingId(Long bookingId);
    List<Long> getOccupiedSeatIds(Long tripId);
    Ticket updateTicketStatus(Long id, String status);
    String generateQrCodeForTicket(Long id);

    // Soát vé & Chốt danh sách:
    CheckInResponse checkInTicket(CheckInRequest request);
    VerifyTicketResponse verifyTicket(VerifyTicketRequest request);
    List<PassengerManifestDto> getPassengerManifest(Long tripId);
    Ticket changeSeat(Long ticketId, Long newSeatId);
    Ticket cancelSingleTicket(Long ticketId, String reason);

    // Nghiệp vụ xử lý khách đến trễ / vắng mặt (No-Show):
    Ticket markTicketAsNoShow(Long ticketId);
    NoShowSummaryDto markTripNoShows(Long tripId);
    int autoMarkPastDepartureNoShows();

    // Chính sách 1: Hủy hoàn toàn vé No-Show (không hoàn tiền)
    Ticket cancelNoShowTicket(Long ticketId, String reason);

    // Chính sách 2: Đổi sang chuyến tiếp theo nếu còn chỗ (có tính phí)
    List<RescheduleOptionDto> getRescheduleOptions(Long ticketId);
    RescheduleTicketResponse rescheduleNoShowTicket(Long ticketId, RescheduleTicketRequest request);
}
