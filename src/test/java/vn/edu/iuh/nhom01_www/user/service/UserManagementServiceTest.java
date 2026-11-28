package vn.edu.iuh.nhom01_www.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.payment.service.RevenueReportService;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.dto.StaffRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.user.service.impl.UserServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    @Mock StaffRepository staffRepository;
    @Mock CustomerRepository customerRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BookingRepository bookingRepository;
    @Mock TripRepository tripRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock RevenueReportService revenueReportService;
    @InjectMocks UserServiceImpl service;

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void createsStaffWithEncodedPasswordAndDefaultStaffRole() {
        var input = input();
        when(roleRepository.findByName("ROLE_STAFF")).thenReturn(Optional.of(Role.builder().id(1L).name("ROLE_STAFF").build()));
        when(passwordEncoder.encode(input.getPassword())).thenReturn("hash");
        when(staffRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var staff = service.createStaff(input);
        assertEquals("staff@example.com", staff.getEmail());
        assertEquals("hash", staff.getPassword());
        assertEquals("ROLE_STAFF", staff.getRole().getName());
        assertEquals("ACTIVE", staff.getStatus());
        assertTrue(staff.getStaffCode().startsWith("NV"));
    }

    @Test void refusesEmailCollisionsAcrossCustomerAndStaffAccounts() {
        when(customerRepository.findByEmailIgnoreCase("staff@example.com")).thenReturn(Customer.builder().build());
        assertThrows(CustomException.class, () -> service.createStaff(input()));
        verify(staffRepository, never()).save(any());
    }

    @Test void refusesDuplicateStaffCode() {
        var input = input();
        input.setStaffCode("NV001");
        when(staffRepository.findByStaffCodeIgnoreCase("NV001")).thenReturn(Staff.builder().id(UUID.randomUUID()).build());
        assertThrows(CustomException.class, () -> service.createStaff(input));
    }

    @Test void refusesUnknownPermissions() {
        var input = input();
        input.setPermissions(List.of("UNKNOWN"));
        assertThrows(CustomException.class, () -> service.createStaff(input));
    }

    @Test void refusesCustomerRoleForStaffAccount() {
        var input = input();
        input.setRoleId(3L);
        when(roleRepository.findById(3L)).thenReturn(Optional.of(Role.builder().name("ROLE_CUSTOMER").build()));
        assertThrows(CustomException.class, () -> service.createStaff(input));
    }

    @Test void cannotLockOwnAccount() {
        UUID id = UUID.randomUUID();
        var staff = Staff.builder().id(id).email("admin@example.com").status("ACTIVE").build();
        when(staffRepository.findById(id)).thenReturn(Optional.of(staff));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.com", "x"));
        assertThrows(CustomException.class, () -> service.updateStaffStatus(id, "LOCKED"));
        assertEquals("ACTIVE", staff.getStatus());
    }

    @Test void locksCustomerAndRejectsUnknownStatus() {
        UUID id = UUID.randomUUID();
        var customer = Customer.builder().id(id).status("ACTIVE").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        assertEquals("LOCKED", service.updateCustomerStatus(id, "LOCKED").getStatus());
        assertThrows(CustomException.class, () -> service.updateCustomerStatus(id, "ANY"));
    }

    @Test void resetPasswordDoesNotUsePublicDefaultPasswords() {
        UUID id = UUID.randomUUID();
        var customer = Customer.builder().id(id).build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("Unique-password-123")).thenReturn("hash");
        service.resetCustomerPassword(id, "Unique-password-123");
        assertEquals("hash", customer.getPassword());
        assertThrows(CustomException.class, () -> service.resetCustomerPassword(id, "123"));
    }

    private StaffRequest input() {
        var input = new StaffRequest();
        input.setEmail("staff@example.com");
        input.setFullName("New Staff");
        input.setPassword("Unique-password-123");
        return input;
    }
}
