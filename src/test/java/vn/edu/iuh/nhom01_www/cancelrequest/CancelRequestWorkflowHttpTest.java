package vn.edu.iuh.nhom01_www.cancelrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.controller.CancelRequestController;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.service.impl.CancelRequestServiceImpl;
import vn.edu.iuh.nhom01_www.common.config.SecurityConfig;
import vn.edu.iuh.nhom01_www.common.exception.GlobalExceptionHandler;
import vn.edu.iuh.nhom01_www.common.security.JwtAuthenticationFilter;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.UserDetailsServiceImpl;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real controllers, services, identity lookup and serialization; only persistence is mocked. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CancelRequestWorkflowHttpTest.TestConfig.class)
@TestPropertySource(properties = "jwt.expiration=3600000")
class CancelRequestWorkflowHttpTest {
    @Autowired WebApplicationContext context;
    @Autowired BookingRepository bookings;
    @Autowired TicketRepository tickets;
    @Autowired CancelRequestRepository requests;
    @Autowired CustomerRepository customers;
    @Autowired StaffRepository staffs;
    MockMvc mvc;
    Booking booking;
    Ticket ticket;
    Staff staff;
    AtomicReference<CancelRequest> stored;

    @BeforeEach
    void setUp() {
        reset(bookings, tickets, requests, customers, staffs);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Customer customer = Customer.builder().id(UUID.randomUUID()).fullName("Khách gửi yêu cầu")
                .email("customer@example.com").password("secret-customer-credential").build();
        staff = Staff.builder().id(UUID.randomUUID()).fullName("Nhân viên tiếp nhận")
                .email("staff@example.com").staffCode("NV123").password("secret-staff-credential").build();
        Trip trip = Trip.builder().id(10L).status("SCHEDULED")
                .departureTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2))
                .route(Route.builder().id(1L).departureLocation("TP.HCM").arrivalLocation("Đà Lạt").build()).build();
        booking = Booking.builder().id(100L).customer(customer).trip(trip).status("PAID")
                .totalPrice(new BigDecimal("250000")).createdAt(LocalDateTime.now()).build();
        ticket = Ticket.builder().id(1000L).booking(booking).ticketCode("VX1000").status("ISSUED")
                .seat(Seat.builder().id(5L).seatName("A01").build()).price(new BigDecimal("250000")).build();
        booking.setTickets(List.of(ticket));
        stored = new AtomicReference<>();

        when(customers.findOptionalByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(staffs.findByEmail("staff@example.com")).thenReturn(staff);
        when(bookings.findOwnedByIdForUpdate(100L, customer.getId())).thenReturn(Optional.of(booking));
        when(bookings.findByIdForUpdate(100L)).thenReturn(Optional.of(booking));
        when(tickets.findByBookingIdForUpdate(100L)).thenReturn(List.of(ticket));
        when(requests.findByBooking_Id(100L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(requests.findByIdForUpdate(20L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(requests.save(any())).thenAnswer(invocation -> {
            CancelRequest request = invocation.getArgument(0);
            request.setId(20L);
            if (request.getCreatedAt() == null) request.setCreatedAt(LocalDateTime.now());
            stored.set(request);
            return request;
        });
        when(requests.findByStatusOrderByCreatedAtAsc(anyString())).thenAnswer(invocation ->
                stored.get() != null && stored.get().getStatus().equals(invocation.getArgument(0))
                        ? List.of(stored.get()) : List.of());
        when(requests.findAllByOrderByCreatedAtDesc()).thenAnswer(invocation ->
                stored.get() == null ? List.of() : List.of(stored.get()));
    }

    private void submitCustomerRequest() throws Exception {
        mvc.perform(post("/api/cancel-requests").with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":100,\"reason\":\"Khách có việc bận\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.reason").value("Khách có việc bận"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.booking.status").value("PAID"))
                .andExpect(jsonPath("$.data.booking.customer.password").doesNotExist())
                .andExpect(content().string(not(containsString("secret-customer-credential"))));
    }

    @Test
    void customerSubmitsStaffReceivesAndApprovesWithRealLoggedInIdentity() throws Exception {
        submitCustomerRequest();
        assertEquals("ISSUED", ticket.getStatus());

        mvc.perform(get("/api/cancel-requests").with(user("staff@example.com").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].booking.seats[0]").value("A01"))
                .andExpect(jsonPath("$.data[0].booking.trip.route.arrivalLocation").value("Đà Lạt"));

        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff@example.com").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isApproved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.booking.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.staff.id").value(staff.getId().toString()))
                .andExpect(jsonPath("$.data.staff.fullName").value("Nhân viên tiếp nhận"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.staff.password").doesNotExist())
                .andExpect(content().string(not(containsString("secret-staff-credential"))));

        assertEquals("CANCELLED", ticket.getStatus());
        mvc.perform(get("/api/cancel-requests").with(user("staff@example.com").roles("STAFF")))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/cancel-requests").param("status", "ALL")
                        .with(user("staff@example.com").roles("STAFF")))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    void staffRejectionKeepsTicketsUsableAndPreservesTheCustomerReason() throws Exception {
        submitCustomerRequest();
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff@example.com").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isApproved\":false,\"rejectReason\":\"Thông tin cần xác minh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reason").value("Khách có việc bận"))
                .andExpect(jsonPath("$.data.rejectReason").value("Thông tin cần xác minh"));
        assertEquals("PAID", booking.getStatus());
        assertEquals("ISSUED", ticket.getStatus());
    }

    @Test
    void resubmittingWhilePendingDoesNotCreateASecondRequest() throws Exception {
        submitCustomerRequest();
        submitCustomerRequest();
        verify(requests, times(1)).save(any());
    }

    @Test
    void ticketCheckedInWhileWaitingCannotBeCancelledByApproval() throws Exception {
        submitCustomerRequest();
        ticket.use();
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff@example.com").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isApproved\":true}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        assertEquals("USED", ticket.getStatus());
        assertEquals("PAID", booking.getStatus());
        assertEquals("PENDING", stored.get().getStatus());
    }

    @Test
    void customerCannotSubmitARequestForAnotherCustomersBooking() throws Exception {
        Customer other = Customer.builder().id(UUID.randomUUID()).email("other@example.com").build();
        when(customers.findOptionalByEmail("other@example.com")).thenReturn(Optional.of(other));
        mvc.perform(post("/api/cancel-requests").with(user("other@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":100,\"reason\":\"Lý do\"}"))
                .andExpect(status().isNotFound());
        assertNull(stored.get());
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, CancelRequestController.class, CancelRequestServiceImpl.class,
            CurrentUserService.class, GlobalExceptionHandler.class})
    static class TestConfig implements WebMvcConfigurer {
        @Bean BookingRepository bookings() { return mock(BookingRepository.class); }
        @Bean TicketRepository tickets() { return mock(TicketRepository.class); }
        @Bean CancelRequestRepository requests() { return mock(CancelRequestRepository.class); }
        @Bean CustomerRepository customers() { return mock(CustomerRepository.class); }
        @Bean StaffRepository staffs() { return mock(StaffRepository.class); }
        @Bean UserDetailsServiceImpl users() { return mock(UserDetailsServiceImpl.class); }
        @Bean JwtUtils jwt() { return mock(JwtUtils.class); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtUtils jwt, UserDetailsServiceImpl users) {
            return new JwtAuthenticationFilter(jwt, users);
        }
        @Bean LocalValidatorFactoryBean requestValidator() { return new LocalValidatorFactoryBean(); }
        @Override public org.springframework.validation.Validator getValidator() { return requestValidator(); }
    }
}
