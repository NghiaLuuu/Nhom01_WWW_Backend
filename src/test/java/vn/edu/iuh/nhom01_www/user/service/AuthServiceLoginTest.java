package vn.edu.iuh.nhom01_www.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.CustomUserDetails;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.UserDetailsServiceImpl;
import vn.edu.iuh.nhom01_www.common.security.model.RefreshToken;
import vn.edu.iuh.nhom01_www.common.security.service.RefreshTokenService;
import vn.edu.iuh.nhom01_www.common.service.EmailService;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.user.dto.JwtResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.user.service.impl.AuthServiceImpl;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private EmailService emailService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginAcceptsLegacyEmailFieldAndReturnsAdminPayload() throws Exception {
        LoginRequest request = new ObjectMapper().readValue(
                "{\"email\":\"admin@vexe.vn\",\"password\":\"password123\"}",
                LoginRequest.class);
        Staff staff = activeStaff("ROLE_ADMIN");
        Authentication authentication = successfulAuthentication(staff, "access-token", "refresh-token");

        when(staffRepository.findByEmailIgnoreCase("admin@vexe.vn")).thenReturn(staff);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        JwtResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals(staff.getId(), response.getUser().getId());
        assertEquals("admin@vexe.vn", response.getUser().getUsername());
        assertEquals("admin", response.getUser().getRole());
    }

    @Test
    void loginAcceptsStaffCodeButAuthenticatesWithStaffEmail() {
        LoginRequest request = loginRequest("STAFF001");
        Staff staff = activeStaff("ROLE_STAFF");
        Authentication authentication = successfulAuthentication(staff, "access-token", "refresh-token");
        when(staffRepository.findByStaffCodeIgnoreCase("STAFF001")).thenReturn(staff);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        JwtResponse response = authService.login(request);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals(staff.getEmail(), captor.getValue().getPrincipal());
        assertEquals("STAFF001", response.getUser().getUsername());
        assertEquals("staff", response.getUser().getRole());
    }

    @Test
    void customerIsAlwaysRejectedWithForbidden() {
        LoginRequest request = loginRequest("customer@example.com");
        when(customerRepository.findByEmail("customer@example.com"))
                .thenReturn(Customer.builder().email("customer@example.com").build());

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void inactiveOrLockedStaffIsRejectedWithForbidden() {
        LoginRequest request = loginRequest("admin@vexe.vn");
        Staff staff = activeStaff("ROLE_ADMIN");
        staff.setStatus("LOCKED");
        when(staffRepository.findByEmailIgnoreCase("admin@vexe.vn")).thenReturn(staff);

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void unsupportedStaffRoleIsRejectedWithForbidden() {
        LoginRequest request = loginRequest("admin@vexe.vn");
        Staff staff = activeStaff("ROLE_DRIVER");
        when(staffRepository.findByEmailIgnoreCase("admin@vexe.vn")).thenReturn(staff);

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void unknownUsernameIsRejectedAsBadCredentials() {
        LoginRequest request = loginRequest("unknown");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void wrongPasswordPropagatesBadCredentials() {
        LoginRequest request = loginRequest("admin@vexe.vn");
        Staff staff = activeStaff("ROLE_ADMIN");
        when(staffRepository.findByEmailIgnoreCase("admin@vexe.vn")).thenReturn(staff);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void lockedAccountCannotRefreshTokensEvenIfVersionMatches() {
        var request = new vn.edu.iuh.nhom01_www.user.dto.TokenRefreshRequest();
        request.setRefreshToken("old-refresh");
        var token = RefreshToken.builder().token("old-refresh").userEmail("locked@example.com").version(4L).build();
        when(refreshTokenService.findByToken("old-refresh")).thenReturn(java.util.Optional.of(token));
        when(refreshTokenService.verifyExpiration(token)).thenReturn(token);
        when(userDetailsService.loadUserByUsername("locked@example.com")).thenReturn(new CustomUserDetails(
                "locked@example.com", "hash", false, true, true, false, List.of(), 4L));
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(CustomException.class, () -> authService.refreshToken(request)).getStatus());
        verify(refreshTokenService).deleteByToken(token);
        verify(jwtUtils, never()).generateToken(any());
    }

    private LoginRequest loginRequest(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("password123");
        return request;
    }

    private Staff activeStaff(String roleName) {
        return Staff.builder()
                .id(UUID.randomUUID())
                .email("admin@vexe.vn")
                .staffCode("STAFF001")
                .password("encoded-password")
                .status("ACTIVE")
                .role(Role.builder().name(roleName).build())
                .version(4L)
                .build();
    }

    private Authentication successfulAuthentication(
            Staff staff,
            String accessToken,
            String refreshTokenValue) {
        CustomUserDetails principal = new CustomUserDetails(
                staff.getEmail(),
                staff.getPassword(),
                List.of(new SimpleGrantedAuthority(staff.getRole().getName())),
                staff.getVersion());
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtUtils.generateToken(principal)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(staff.getEmail(), staff.getVersion()))
                .thenReturn(RefreshToken.builder().token(refreshTokenValue).build());
        return authentication;
    }
}
