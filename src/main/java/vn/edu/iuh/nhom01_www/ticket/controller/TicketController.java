package vn.edu.iuh.nhom01_www.ticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.ticket.dto.*;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.service.TicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", ticketService.getTicketById(id)));
    }

    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<ApiResponse<Ticket>> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", ticketService.getTicketByCode(ticketCode)));
    }
    
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<Ticket>>> getTicketsByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", ticketService.getTicketsByBookingId(bookingId)));
    }

    @GetMapping("/occupied-seats/{tripId}")
    public ResponseEntity<ApiResponse<List<Long>>> getOccupiedSeatsByTripId(@PathVariable Long tripId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", ticketService.getOccupiedSeatIds(tripId)));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<ApiResponse<String>> getTicketQrCode(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "QR code generated", ticketService.generateQrCodeForTicket(id)));
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Ticket>> updateTicketStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket updated", ticketService.updateTicketStatus(id, body.get("status"))));
    }

    /**
     * Soát vé / Check-in lên xe (Dành cho Tài xế, Phụ xe, Nhân viên soát vé)
     * Nhận vào mã vé hoặc toàn bộ nội dung quét từ mã QR code.
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkInTicket(@Valid @RequestBody CheckInRequest request) {
        CheckInResponse response = ticketService.checkInTicket(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response.getMessage(), response));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<VerifyTicketResponse> verifyTicket(@RequestBody VerifyTicketRequest request) {
        return ResponseEntity.ok(ticketService.verifyTicket(request));
    }

    /**
     * Danh sách hành khách theo chuyến xe (Passenger Manifest)
     * Phục vụ tài xế/phụ xe điểm danh danh sách hành khách trước khi khởi hành.
     */
    @GetMapping("/trip/{tripId}/manifest")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasAuthority('TRIP_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<PassengerManifestDto>>> getPassengerManifest(@PathVariable Long tripId) {
        List<PassengerManifestDto> manifest = ticketService.getPassengerManifest(tripId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách hành khách thành công", manifest));
    }

    /**
     * Đổi ghế ngồi cùng chuyến cho vé đã mua
     */
    @PostMapping("/{id}/change-seat")
    public ResponseEntity<ApiResponse<Ticket>> changeSeat(@PathVariable Long id, @Valid @RequestBody ChangeSeatRequest request) {
        Ticket updatedTicket = ticketService.changeSeat(id, request.getNewSeatId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Đổi ghế ngồi thành công!", updatedTicket));
    }

    /**
     * Hủy vé lẻ cụ thể trong đơn đặt vé (Quy trình trước giờ đóng cổng)
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'TICKET_MANAGE', 'MANAGE_TICKET')")
    public ResponseEntity<ApiResponse<Ticket>> cancelSingleTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Khách yêu cầu hủy vé";
        Ticket cancelledTicket = ticketService.cancelSingleTicket(id, reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Hủy vé thành công!", cancelledTicket));
    }

    // ==================== API XỬ LÝ KHÁCH ĐẾN TRỄ / VẮNG MẶT (NO-SHOW) ====================

    /**
     * Đánh dấu 1 vé cụ thể là VẮNG MẶT (NO_SHOW)
     */
    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Ticket>> markTicketAsNoShow(@PathVariable Long id) {
        Ticket ticket = ticketService.markTicketAsNoShow(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã đánh dấu vé là Vắng mặt (NO-SHOW)!", ticket));
    }

    /**
     * Chốt danh sách xuất bến: Đánh dấu tất cả vé chưa lên xe của chuyến là NO_SHOW
     */
    @PostMapping("/trip/{tripId}/mark-no-show")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<NoShowSummaryDto>> markTripNoShows(@PathVariable Long tripId) {
        NoShowSummaryDto summary = ticketService.markTripNoShows(tripId);
        return ResponseEntity.ok(new ApiResponse<>(true, summary.getMessage(), summary));
    }

    /**
     * Kích hoạt quét tự động No-Show thủ công
     */
    @PostMapping("/auto-mark-no-shows")
    @PreAuthorize("hasAuthority('TICKET_MANAGE') or hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoMarkPastDepartureNoShows() {
        int count = ticketService.autoMarkPastDepartureNoShows();
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã cập nhật " + count + " vé sang trạng thái NO_SHOW.", Map.of("updatedCount", count)));
    }

    /**
     * Tra cứu danh sách các chuyến xe tiếp theo còn chỗ cùng tuyến đường & ước tính chi phí đổi vé
     */
    @GetMapping("/{id}/reschedule-options")
    public ResponseEntity<ApiResponse<List<RescheduleOptionDto>>> getRescheduleOptions(@PathVariable Long id) {
        List<RescheduleOptionDto> options = ticketService.getRescheduleOptions(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách chuyến tiếp theo thành công", options));
    }

    /**
     * CHÍNH SÁCH 1: Hủy hoàn toàn vé No-Show (không hoàn tiền cho khách đến trễ/vắng mặt)
     */
    @PostMapping("/{id}/no-show/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'TICKET_MANAGE', 'MANAGE_TICKET')")
    public ResponseEntity<ApiResponse<Ticket>> cancelNoShowTicket(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Hành khách đến trễ / vắng mặt";
        Ticket ticket = ticketService.cancelNoShowTicket(id, reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vé đã bị hủy hoàn toàn theo chính sách No-Show (Không hoàn tiền).", ticket));
    }

    /**
     * CHÍNH SÁCH 2: Đổi sang chuyến xe tiếp theo nếu còn chỗ (có tính phí đổi vé + chênh lệch giá)
     */
    @PostMapping("/{id}/no-show/reschedule")
    public ResponseEntity<ApiResponse<RescheduleTicketResponse>> rescheduleNoShowTicket(
            @PathVariable Long id,
            @RequestBody(required = false) RescheduleTicketRequest request) {
        RescheduleTicketResponse response = ticketService.rescheduleNoShowTicket(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response.getMessage(), response));
    }
}
