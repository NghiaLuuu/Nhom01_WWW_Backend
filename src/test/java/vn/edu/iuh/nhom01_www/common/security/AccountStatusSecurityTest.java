package vn.edu.iuh.nhom01_www.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountStatusSecurityTest {
    @Mock CustomerRepository customerRepository;
    @Mock StaffRepository staffRepository;
    @InjectMocks UserDetailsServiceImpl details;

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void lockedCustomerIsNotEnabled() {
        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Customer.builder()
                .email("customer@example.com").password("hash").status("LOCKED").build());
        var result = details.loadUserByUsername("customer@example.com");
        assertFalse(result.isEnabled());
        assertFalse(result.isAccountNonLocked());
    }

    @Test void lockedStaffIsNotEnabled() {
        when(staffRepository.findByEmail("staff@example.com")).thenReturn(Staff.builder()
                .email("staff@example.com").password("hash").status("LOCKED").build());
        assertFalse(details.loadUserByUsername("staff@example.com").isEnabled());
    }

    @Test void activeAccountRemainsEnabled() {
        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Customer.builder()
                .email("customer@example.com").password("hash").status("ACTIVE").build());
        assertTrue(details.loadUserByUsername("customer@example.com").isEnabled());
    }

    @Test void customerPasswordIsNeverSerialized() throws Exception {
        var customer = Customer.builder().email("customer@example.com").password("secret-hash").build();
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(customer);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test void validJwtCannotAuthenticateLockedAccount() throws Exception {
        var jwt = mock(JwtUtils.class);
        var users = mock(UserDetailsServiceImpl.class);
        var filter = new JwtAuthenticationFilter(jwt, users);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwt.validateToken("valid-token")).thenReturn(true);
        when(jwt.getUsernameFromToken("valid-token")).thenReturn("locked@example.com");
        when(users.loadUserByUsername("locked@example.com")).thenReturn(new CustomUserDetails(
                "locked@example.com", "hash", false, true, true, false, List.of(), 0L));
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
