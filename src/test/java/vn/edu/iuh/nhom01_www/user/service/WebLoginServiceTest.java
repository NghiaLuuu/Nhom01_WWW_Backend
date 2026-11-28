package vn.edu.iuh.nhom01_www.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.CustomUserDetails;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.model.RefreshToken;
import vn.edu.iuh.nhom01_www.common.security.service.RefreshTokenService;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.user.service.impl.AuthServiceImpl;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebLoginServiceTest {
    @Mock CustomerRepository customers;
    @Mock StaffRepository staffs;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils jwtUtils;
    @Mock RefreshTokenService refreshTokens;
    @InjectMocks AuthServiceImpl service;

    @Test void activeCustomerCanLoginOnWebWithCanonicalEmail() {
        var customer = Customer.builder().id(UUID.randomUUID()).email("customer@example.com").status("ACTIVE").version(2L).build();
        when(customers.findByEmailIgnoreCase("CUSTOMER@example.com")).thenReturn(customer);
        var principal = new CustomUserDetails(customer.getEmail(), "hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), 2L);
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtUtils.generateToken(principal)).thenReturn("access");
        when(refreshTokens.createRefreshToken(customer.getEmail(), 2L)).thenReturn(RefreshToken.builder().token("refresh").build());
        var response = service.webLogin(request("CUSTOMER@example.com"));
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("customer", response.getUser().getRole());
        assertEquals(customer.getId(), response.getUser().getId());
        verifyNoInteractions(staffs);
    }

    @Test void lockedCustomerCannotLoginOnWeb() {
        when(customers.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Customer.builder().status("LOCKED").build());
        assertThrows(CustomException.class, () -> service.webLogin(request("customer@example.com")));
        verifyNoInteractions(authenticationManager, jwtUtils, refreshTokens);
    }

    private LoginRequest request(String username) {
        var request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("password123");
        return request;
    }
}
