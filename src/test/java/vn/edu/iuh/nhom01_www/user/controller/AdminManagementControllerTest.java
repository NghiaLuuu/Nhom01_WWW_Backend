package vn.edu.iuh.nhom01_www.user.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.cancelrequest.controller.CancelRequestController;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;
import vn.edu.iuh.nhom01_www.common.exception.GlobalExceptionHandler;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.service.RevenueReportService;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.service.AdminManagementService;
import vn.edu.iuh.nhom01_www.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminManagementControllerTest {
    AnnotationConfigApplicationContext context;
    MockMvc mvc;
    AdminManagementService management;
    CancelRequestService cancellations;

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        management = context.getBean(AdminManagementService.class);
        cancellations = context.getBean(CancelRequestService.class);
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(AdminController.class), context.getBean(CancelRequestController.class),
                        context.getBean(vn.edu.iuh.nhom01_www.ticket.controller.TicketController.class))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test void adminCanSearchCustomers() throws Exception {
        authenticate("ROLE_ADMIN");
        when(management.customers("An", null, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/admin/customers").param("keyword", "An"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        verify(management).customers("An", null, 0, 20);
    }

    @Test void customerCannotListStaff() throws Exception {
        authenticate("ROLE_CUSTOMER");
        mvc.perform(get("/api/admin/staffs")).andExpect(status().isForbidden());
        verifyNoInteractions(management);
    }

    @Test void delegatedStaffCannotGrantAdminRoles() throws Exception {
        authenticate("STAFF_MANAGE");
        mvc.perform(post("/api/admin/staffs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"fullName\":\"Staff\",\"password\":\"Safe-pass-123\",\"roleId\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(UserService.class));
    }

    @Test void invalidAccountStatusReturnsBadRequest() throws Exception {
        authenticate("ROLE_ADMIN");
        mvc.perform(patch("/api/admin/customers/" + UUID.randomUUID() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void invalidEmailAndWeakPasswordReturnBadRequest() throws Exception {
        authenticate("ROLE_ADMIN");
        mvc.perform(post("/api/admin/staffs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void customerCannotResolveCancellation() throws Exception {
        authenticate("ROLE_CUSTOMER");
        mvc.perform(patch("/api/cancel-requests/1/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isApproved\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cancellations);
    }

    @Test void resolveRequiresExplicitDecision() throws Exception {
        authenticate("TICKET_MANAGE");
        mvc.perform(patch("/api/cancel-requests/1/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cancellations);
    }

    @Test void resolverComesFromAuthenticatedStaffNotBody() throws Exception {
        authenticate("TICKET_MANAGE");
        UUID actor = UUID.randomUUID();
        when(context.getBean(CurrentUserService.class).getCurrentStaff()).thenReturn(Staff.builder().id(actor).build());
        when(cancellations.resolveCancelRequest(1L, actor, false, "Reason"))
                .thenThrow(new vn.edu.iuh.nhom01_www.common.exception.CustomException("Not found", org.springframework.http.HttpStatus.NOT_FOUND));
        mvc.perform(patch("/api/cancel-requests/1/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isApproved\":false,\"rejectReason\":\"Reason\"}"))
                .andExpect(status().isNotFound());
        verify(cancellations).resolveCancelRequest(1L, actor, false, "Reason");
    }

    @Test void revenueAcceptsDateRangeAndGrouping() throws Exception {
        authenticate("VIEW_STATISTICS");
        mvc.perform(get("/api/admin/statistics/revenue").param("from", "2026-10-01")
                .param("to", "2026-10-31").param("groupBy", "MONTH")).andExpect(status().isOk());
        verify(context.getBean(RevenueReportService.class)).report(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), RevenueReportService.GroupBy.MONTH);
    }

    @Test void customerCanReadOwnCancellationListNotStaffQueue() throws Exception {
        authenticate("ROLE_CUSTOMER");
        when(cancellations.mine(null, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/cancel-requests/mine").param("customerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        verify(cancellations).mine(null, 0, 20);
        mvc.perform(get("/api/cancel-requests")).andExpect(status().isForbidden());
    }

    @Test void staffCannotSubmitCustomerRequestOrReadMine() throws Exception {
        authenticate("ROLE_STAFF");
        mvc.perform(post("/api/cancel-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookingId\":1,\"reason\":\"Changed plans\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/cancel-requests/mine")).andExpect(status().isForbidden());
        verifyNoInteractions(cancellations);
    }

    @Test void authorizedTicketManagerCanFilterCancellationQueue() throws Exception {
        authenticate("MANAGE_TICKET");
        when(cancellations.search("PENDING", 123L, 1, 10)).thenReturn(new PageResponse<>(List.of(), 1, 10, 0, 0));
        mvc.perform(get("/api/cancel-requests").param("status", "PENDING").param("bookingId", "123")
                .param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray());
        verify(cancellations).search("PENDING", 123L, 1, 10);
    }

    @Test void customerDetailRouteUsesOwnerRestrictedService() throws Exception {
        authenticate("ROLE_CUSTOMER");
        mvc.perform(get("/api/cancel-requests/mine/4")).andExpect(status().isOk());
        verify(cancellations).myDetail(4L);
        verify(cancellations, never()).detail(any());
    }

    @Test void staffWithoutReportPermissionCannotReadRevenue() throws Exception {
        authenticate("ROLE_STAFF");
        mvc.perform(get("/api/admin/statistics/revenue")).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(RevenueReportService.class));
    }

    @Test void customerCannotBypassStaffApprovalByCancellingTicketDirectly() throws Exception {
        authenticate("ROLE_CUSTOMER");
        mvc.perform(post("/api/tickets/3/cancel")).andExpect(status().isForbidden());
        mvc.perform(post("/api/tickets/3/no-show/cancel")).andExpect(status().isForbidden());
        verifyNoInteractions(context.getBean(vn.edu.iuh.nhom01_www.ticket.service.TicketService.class));
    }

    @Test void ordinaryStaffCannotResolveWithoutTicketPermission() throws Exception {
        authenticate("ROLE_STAFF");
        mvc.perform(patch("/api/cancel-requests/4/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isApproved\":true}")).andExpect(status().isForbidden());
        verifyNoInteractions(cancellations);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "actor@example.com", "", List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean UserService users() { return mock(UserService.class); }
        @Bean AdminManagementService management() { return mock(AdminManagementService.class); }
        @Bean RevenueReportService revenue() { return mock(RevenueReportService.class); }
        @Bean BookingService bookings() { return mock(BookingService.class); }
        @Bean vn.edu.iuh.nhom01_www.ticket.service.TicketService tickets() { return mock(vn.edu.iuh.nhom01_www.ticket.service.TicketService.class); }
        @Bean vn.edu.iuh.nhom01_www.ticket.controller.TicketController ticketController(vn.edu.iuh.nhom01_www.ticket.service.TicketService tickets) {
            return new vn.edu.iuh.nhom01_www.ticket.controller.TicketController(tickets);
        }
        @Bean CancelRequestService cancellations() { return mock(CancelRequestService.class); }
        @Bean CurrentUserService currentUser() { return mock(CurrentUserService.class); }
        @Bean AdminController adminController(UserService users, AdminManagementService management,
                RevenueReportService revenue, BookingService bookings) { return new AdminController(users, management, revenue, bookings); }
        @Bean CancelRequestController cancellationController(CancelRequestService cancellations, CurrentUserService currentUser) {
            return new CancelRequestController(cancellations, currentUser);
        }
    }
}
