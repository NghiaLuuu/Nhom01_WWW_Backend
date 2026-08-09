package vn.edu.iuh.nhom01_www.ticket.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
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
    
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<Ticket>>> getTicketsByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", ticketService.getTicketsByBookingId(bookingId)));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Ticket>> updateTicketStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket updated", ticketService.updateTicketStatus(id, body.get("status"))));
    }
}
