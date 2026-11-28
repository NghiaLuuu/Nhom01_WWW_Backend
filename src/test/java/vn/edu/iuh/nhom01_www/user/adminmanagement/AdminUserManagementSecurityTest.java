package vn.edu.iuh.nhom01_www.user.adminmanagement;

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
import vn.edu.iuh.nhom01_www.user.adminmanagement.controller.AdminUserManagementController;
import vn.edu.iuh.nhom01_www.user.adminmanagement.service.AdminUserManagementService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real method-security proxy, without starting Boot, a datasource, or any seeders. */
class AdminUserManagementSecurityTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnonymousRequests() {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            var controller = context.getBean(AdminUserManagementController.class);
            assertThatThrownBy(controller::options).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            verifyNoInteractions(context.getBean(AdminUserManagementService.class));
        }
    }

    @Test
    void rejectsCustomersAndStaffEvenWithManagementPermissions() {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            var controller = context.getBean(AdminUserManagementController.class);
            for (String role : List.of("ROLE_CUSTOMER", "ROLE_STAFF")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user@example.com", null, List.of(new SimpleGrantedAuthority(role), new SimpleGrantedAuthority("STAFF_MANAGE"))));
                assertThatThrownBy(controller::options).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.customers("", null, 0, 10)).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(context.getBean(AdminUserManagementService.class));
        }
    }

    @Test
    void allowsAdministratorRequests() {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            context.getBean(AdminUserManagementController.class).options();
            verify(context.getBean(AdminUserManagementService.class)).options();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityConfiguration {
        @Bean
        AdminUserManagementService managementService() {
            return mock(AdminUserManagementService.class);
        }

        @Bean
        AdminUserManagementController managementController(AdminUserManagementService service) {
            return new AdminUserManagementController(service);
        }
    }
}
