package vn.edu.iuh.nhom01_www.ticket.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.ticket.service.TicketService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    
    @Override
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new CustomException("Ticket not found", HttpStatus.NOT_FOUND));
    }
    
    @Override
    public List<Ticket> getTicketsByBookingId(Long bookingId) {
        return ticketRepository.findByBookingId(bookingId);
    }
    
    @Override
    @Transactional
    public Ticket updateTicketStatus(Long id, String status) {
        Ticket ticket = getTicketById(id);
        if ("USED".equalsIgnoreCase(status)) {
            ticket.use();
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            ticket.cancel();
        } else {
            throw new CustomException("Invalid status", HttpStatus.BAD_REQUEST);
        }
        return ticketRepository.save(ticket);
    }
}
