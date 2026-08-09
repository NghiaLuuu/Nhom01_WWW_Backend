package vn.edu.iuh.nhom01_www.ticket.service;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import java.util.List;
public interface TicketService {
    Ticket getTicketById(Long id);
    List<Ticket> getTicketsByBookingId(Long bookingId);
    Ticket updateTicketStatus(Long id, String status);
}
