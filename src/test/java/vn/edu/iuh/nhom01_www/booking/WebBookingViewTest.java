package vn.edu.iuh.nhom01_www.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.edu.iuh.nhom01_www.booking.dto.WebBookingView;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WebBookingViewTest {
    @Test void mapsSeatsAndBookingCodeWithoutCustomerSecrets() throws Exception {
        var view = WebBookingView.from(booking(), null);
        assertEquals("BKG-42", view.bookingCode());
        assertEquals(List.of("A1"), view.seats());
        assertTrue(view.canCancel());
        var json = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()).writeValueAsString(view);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test void pendingRequestUsesRequestIdNotBookingId() {
        var booking = booking();
        var view = WebBookingView.from(booking, CancelRequest.builder().id(99L).booking(booking).status("PENDING").reason("Changed plans").build());
        assertEquals("CANCEL_REQUESTED", view.status());
        assertEquals(99L, view.cancelRequestId());
        assertFalse(view.canCancel());
    }

    @Test void rejectedRequestPreservesPaidStatusAndCannotBeSubmittedAgain() {
        var view = WebBookingView.from(booking(), CancelRequest.builder().id(99L).status("REJECTED").rejectReason("Too late").build());
        assertEquals("PAID", view.status());
        assertEquals("Too late", view.rejectReason());
        assertFalse(view.canCancel());
    }

    @Test void usedTicketCannotBeCancelled() {
        var booking = booking();
        booking.getTickets().get(0).setStatus("USED");
        assertFalse(WebBookingView.from(booking, null).canCancel());
    }

    private Booking booking() {
        return Booking.builder().id(42L).status("PAID").totalPrice(BigDecimal.valueOf(250000))
                .customer(Customer.builder().email("customer@example.com").password("secret-hash").build())
                .trip(Trip.builder().id(1L).departureTime(LocalDateTime.now().plusDays(1))
                        .route(Route.builder().id(2L).departureLocation("TP.HCM").arrivalLocation("Đà Lạt").build())
                        .vehicle(Vehicle.builder().id(3L).capacity(36).build()).build())
                .tickets(List.of(Ticket.builder().status("ISSUED").seat(Seat.builder().seatName("A1").build()).build())).build();
    }
}
