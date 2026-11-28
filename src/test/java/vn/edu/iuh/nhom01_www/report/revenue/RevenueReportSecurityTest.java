package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.iuh.nhom01_www.report.revenue.controller.RevenueReportController;
import vn.edu.iuh.nhom01_www.report.revenue.service.RevenueReportService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RevenueReportSecurityTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnonymousRequests() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            assertThatThrownBy(context.getBean(RevenueReportController.class)::options)
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            verifyNoInteractions(context.getBean(RevenueReportService.class));
        }
    }

    @Test
    void rejectsCustomersAndStaffWithoutReportingPermission() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            for (String role : new String[]{"ROLE_CUSTOMER", "ROLE_STAFF"}) {
                login(role);
                assertThatThrownBy(context.getBean(RevenueReportController.class)::options).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(context.getBean(RevenueReportService.class));
        }
    }

    @Test
    void allowsAdminAndExplicitlyAuthorizedStaff() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(RevenueReportController.class);
            login("ROLE_ADMIN"); controller.options();
            login("ROLE_STAFF", "VIEW_STATISTICS"); controller.options();
            verify(context.getBean(RevenueReportService.class), times(2)).options();
        }
    }

    private void login(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reporter@example.test", null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean RevenueReportService service() { return mock(RevenueReportService.class); }
        @Bean RevenueReportController controller(RevenueReportService service) { return new RevenueReportController(service); }
    }
}
