package vn.edu.iuh.nhom01_www.booking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public Booking createBooking(UUID customerId, Long tripId, List<Long> seatIds) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Trip not found", HttpStatus.NOT_FOUND));

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new CustomException("One or more seats not found", HttpStatus.BAD_REQUEST);
        }

        // Simple validation: Ensure seats belong to the trip's vehicle
        for (Seat seat : seats) {
            if (!seat.getVehicle().getId().equals(trip.getVehicle().getId())) {
                throw new CustomException("Seat does not belong to the trip's vehicle", HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal totalPrice = trip.getPrice().multiply(new BigDecimal(seats.size()));

        Booking booking = Booking.builder()
                .customer(customer)
                .trip(trip)
                .status("PENDING_PAYMENT")
                .totalPrice(totalPrice)
                .holdExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        for (Seat seat : seats) {
            Ticket ticket = Ticket.builder()
                    .booking(savedBooking)
                    .seat(seat)
                    .status("PENDING")
                    .price(trip.getPrice())
                    .build();
            tickets.add(ticket);
        }
        
        ticketRepository.saveAll(tickets);
        savedBooking.setTickets(tickets);

        return savedBooking;
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void releaseExpiredBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndHoldExpiresAtBefore("PENDING_PAYMENT", LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            booking.expireBooking();
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.release();
                }
            }
        }
        
        bookingRepository.saveAll(expiredBookings);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
