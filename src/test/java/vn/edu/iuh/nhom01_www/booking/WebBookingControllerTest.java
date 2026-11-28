package vn.edu.iuh.nhom01_www.booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.iuh.nhom01_www.booking.controller.WebBookingController;
import vn.edu.iuh.nhom01_www.booking.service.WebBookingQueryService;
import vn.edu.iuh.nhom01_www.common.exception.GlobalExceptionHandler;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebBookingControllerTest {
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    WebBookingQueryService bookings;
    CurrentUserService currentUser;

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        bookings = context.getBean(WebBookingQueryService.class);
        currentUser = context.getBean(CurrentUserService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(WebBookingController.class))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test void historyAlwaysUsesAuthenticatedCustomerNotQueryParameter() throws Exception {
        authenticate("ROLE_CUSTOMER");
        UUID owner = UUID.randomUUID();
        when(currentUser.getCurrentCustomer()).thenReturn(Customer.builder().id(owner).build());
        when(bookings.history(owner)).thenReturn(List.of());
        mvc.perform(get("/api/web/bookings/my-history").param("customerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(bookings).history(owner);
        verify(bookings, never()).management();
    }

    @Test void customerCannotReadAllBookings() throws Exception {
        authenticate("ROLE_CUSTOMER");
        mvc.perform(get("/api/web/bookings")).andExpect(status().isForbidden());
        verifyNoInteractions(bookings);
    }

    @Test void staffWithoutTicketPermissionCannotReadAllBookings() throws Exception {
        authenticate("ROLE_STAFF");
        mvc.perform(get("/api/web/bookings")).andExpect(status().isForbidden());
        verifyNoInteractions(bookings);
    }

    @Test void ticketManagerCanReadManagementView() throws Exception {
        authenticate("TICKET_MANAGE");
        when(bookings.management()).thenReturn(List.of());
        mvc.perform(get("/api/web/bookings")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test void adminCanReadManagementView() throws Exception {
        authenticate("ROLE_ADMIN");
        when(bookings.management()).thenReturn(List.of());
        mvc.perform(get("/api/web/bookings")).andExpect(status().isOk());
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com", "", List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean WebBookingQueryService bookings() { return mock(WebBookingQueryService.class); }
        @Bean CurrentUserService currentUser() { return mock(CurrentUserService.class); }
        @Bean WebBookingController controller(WebBookingQueryService bookings, CurrentUserService currentUser) {
            return new WebBookingController(bookings, currentUser);
        }
    }
}
