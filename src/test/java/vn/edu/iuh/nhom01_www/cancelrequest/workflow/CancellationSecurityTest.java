package vn.edu.iuh.nhom01_www.cancelrequest.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancellationSecurityTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void deniesAnonymousAndCustomersOnStaffEndpoints() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(StaffCancellationController.class);
            assertThatThrownBy(() -> controller.list("", "", 0, 10)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            login("ROLE_CUSTOMER", "MANAGE_TICKET");
            assertThatThrownBy(() -> controller.details(1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(context.getBean(CancellationWorkflowService.class));
        }
    }

    @Test void requiresTicketPermissionsForStaffButAllowsAdmin() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(StaffCancellationController.class);
            login("ROLE_STAFF");
            assertThatThrownBy(() -> controller.details(1L)).isInstanceOf(AccessDeniedException.class);
            login("ROLE_STAFF", "MANAGE_TICKET"); controller.details(1L);
            login("ROLE_STAFF", "TICKET_MANAGE"); controller.details(1L);
            login("ROLE_ADMIN"); controller.details(1L);
            verify(context.getBean(CancellationWorkflowService.class), times(3)).staffDetails(1L);
        }
    }

    @Test void restrictsCustomerEndpointsToCustomerRole() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CustomerCancellationController.class);
            var profile = context.getBean(ProfileCancellationController.class);
            login("ROLE_STAFF", "TICKET_MANAGE");
            assertThatThrownBy(() -> controller.history("", 0, 10)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> profile.submit(7L)).isInstanceOf(AccessDeniedException.class);
            login("ROLE_CUSTOMER"); controller.history("", 0, 10);
            profile.submit(7L);
            verify(context.getBean(CancellationWorkflowService.class)).myRequests("", 0, 10);
            verify(context.getBean(CancellationWorkflowService.class)).submitFromProfile(7L);
        }
    }

    private void login(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user@example.test", null,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    @Configuration(proxyBeanMethods = false) @EnableMethodSecurity
    static class Config {
        @Bean CancellationWorkflowService service() { return mock(CancellationWorkflowService.class); }
        @Bean StaffCancellationController staff(CancellationWorkflowService service) { return new StaffCancellationController(service); }
        @Bean CustomerCancellationController customer(CancellationWorkflowService service) { return new CustomerCancellationController(service); }
        @Bean ProfileCancellationController profile(CancellationWorkflowService service) { return new ProfileCancellationController(service); }
    }
}
