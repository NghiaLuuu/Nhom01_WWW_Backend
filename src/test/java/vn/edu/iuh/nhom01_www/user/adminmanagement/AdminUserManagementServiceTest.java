package vn.edu.iuh.nhom01_www.user.adminmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.repository.RefreshTokenRepository;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.user.adminmanagement.dto.UserManagementDtos.*;
import vn.edu.iuh.nhom01_www.user.adminmanagement.repository.UserManagementQueries;
import vn.edu.iuh.nhom01_www.user.adminmanagement.service.AdminUserManagementService;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {
    @Mock private StaffRepository staffRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserManagementQueries queries;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private CurrentUserService currentUserService;
    private AdminUserManagementService service;
    private Staff target;
    private Staff actor;
    private Role staffRole;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(staffRepository, customerRepository, roleRepository,
                permissionRepository, queries, passwordEncoder, refreshTokenRepository, currentUserService);
        staffRole = Role.builder().id(2L).name("ROLE_STAFF").permissions(Set.of()).build();
        target = Staff.builder().id(UUID.randomUUID()).fullName("Nhân viên Một").email("staff@example.com")
                .staffCode("NV_01").status("ACTIVE").role(staffRole).password("stored-hash").version(2L).build();
        actor = Staff.builder().id(UUID.randomUUID()).email("admin@example.com").status("ACTIVE")
                .role(Role.builder().id(1L).name("ROLE_ADMIN").build()).build();
        customer = Customer.builder().id(UUID.randomUUID()).fullName("Khách Một").email("customer@example.com")
                .password("stored-hash").status("ACTIVE").version(3L).build();
    }

    @Test
    void createsStaffWithNormalizedIdentifiersAndEncodedPassword() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("SafePass123!")).thenReturn("encoded-password");
        when(staffRepository.saveAndFlush(any(Staff.class))).thenAnswer(invocation -> {
            Staff user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setVersion(0L);
            return user;
        });
        UserAccount result = service.createStaff(new CreateStaffRequest(details("  STAFF@Example.com  ", List.of()), "SafePass123!"));
        assertThat(result.email()).isEqualTo("staff@example.com");
        assertThat(result.staffCode()).isEqualTo("NV_NEW");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(staffRepository).saveAndFlush(argThat(user -> "encoded-password".equals(user.getPassword())));
    }

    @Test
    void rejectsEmailAlreadyUsedIncludingCustomerAccounts() {
        when(queries.emailInUse("staff@example.com", null)).thenReturn(true);
        assertThatThrownBy(() -> service.createStaff(new CreateStaffRequest(details("staff@example.com", List.of()), "SafePass123!")))
                .isInstanceOf(CustomException.class).hasMessageContaining("Email");
        verifyNoInteractions(passwordEncoder, staffRepository);
    }

    @Test
    void rejectsDuplicateStaffCode() {
        when(queries.staffCodeInUse("NV_NEW", null)).thenReturn(true);
        assertThatThrownBy(() -> service.createStaff(new CreateStaffRequest(details("new@example.com", List.of()), "SafePass123!")))
                .hasMessageContaining("Mã nhân viên");
        verifyNoInteractions(staffRepository);
    }

    @Test
    void rejectsUnsupportedStaffRole() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(Role.builder().id(2L).name("ROLE_CUSTOMER").build()));
        assertThatThrownBy(() -> service.createStaff(new CreateStaffRequest(details("new@example.com", List.of()), "SafePass123!")))
                .hasMessageContaining("Admin hoặc Staff");
        verifyNoInteractions(staffRepository);
    }

    @Test
    void rejectsUnknownPermissionCodes() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        assertThatThrownBy(() -> service.createStaff(new CreateStaffRequest(details("new@example.com", List.of("UNKNOWN")), "SafePass123!")))
                .hasMessageContaining("quyền không tồn tại");
    }

    @Test
    void keepsInheritedPermissionsSeparateFromAdditionalPermissions() {
        Permission inherited = Permission.builder().id(1L).code("TRIP_MANAGE").build();
        Permission extra = Permission.builder().id(2L).code("TICKET_MANAGE").build();
        staffRole.setPermissions(Set.of(inherited));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        when(permissionRepository.findAll()).thenReturn(List.of(inherited, extra));
        when(staffRepository.saveAndFlush(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserAccount result = service.createStaff(new CreateStaffRequest(
                details("new@example.com", List.of("TRIP_MANAGE", "TICKET_MANAGE", "TICKET_MANAGE")), "SafePass123!"));
        assertThat(result.permissions()).containsExactly("TICKET_MANAGE");
        assertThat(result.effectivePermissions()).containsExactly("TICKET_MANAGE", "TRIP_MANAGE");
    }

    @Test
    void paginatesAndSerializesWithoutAnyPasswordFields() throws Exception {
        when(queries.findStaff("staff", null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(target), PageRequest.of(0, 10), 1));
        PageResult<UserAccount> result = service.listStaff("staff", "", null, 0, 10);
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(result);
        assertThat(json).doesNotContain("password", "stored-hash");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void preventsChangingOwnStaffAccount() {
        when(staffRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(currentUserService.getCurrentStaff()).thenReturn(target);
        assertThatThrownBy(() -> service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 2L)))
                .hasMessageContaining("đang đăng nhập");
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStaleVersionBeforeWriting() {
        prepareStaff();
        assertThatThrownBy(() -> service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 1L)))
                .isInstanceOfSatisfying(CustomException.class, exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void locksStaffAndRevokesRefreshTokens() {
        prepareStaff();
        when(staffRepository.saveAndFlush(target)).thenReturn(target);
        UserAccount result = service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 2L));
        assertThat(result.status()).isEqualTo("LOCKED");
        verify(refreshTokenRepository).deleteByUserEmail(target.getEmail());
    }

    @Test
    void preventsLockingTheLastActiveAdministrator() {
        target.setRole(actor.getRole());
        prepareStaff();
        when(queries.lockActiveAdministrators()).thenReturn(List.of(target));
        assertThatThrownBy(() -> service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 2L)))
                .hasMessageContaining("ít nhất một Admin");
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void permitsLockingAnAdministratorWhenAnotherActiveAdminRemains() {
        target.setRole(actor.getRole());
        prepareStaff();
        when(queries.lockActiveAdministrators()).thenReturn(List.of(actor, target));
        when(staffRepository.saveAndFlush(target)).thenReturn(target);
        assertThat(service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 2L)).status()).isEqualTo("LOCKED");
    }

    @Test
    void locksCustomerWithoutDeletingTheirHistory() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
        assertThat(service.changeCustomerStatus(customer.getId(), new ChangeStatusRequest("LOCKED", 3L)).status()).isEqualTo("LOCKED");
        verify(refreshTokenRepository).deleteByUserEmail(customer.getEmail());
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void doesNotBypassCustomerOtpVerification() {
        customer.setStatus("PENDING_VERIFICATION");
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.changeCustomerStatus(customer.getId(), new ChangeStatusRequest("ACTIVE", 3L)))
                .hasMessageContaining("chưa xác thực OTP");
    }

    @Test
    void resetsPasswordUsingEncodingAndRevokesSessions() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("BrandNew123!")).thenReturn("new-password-hash");
        service.resetCustomerPassword(customer.getId(), new ResetPasswordRequest("BrandNew123!", 3L));
        assertThat(customer.getPassword()).isEqualTo("new-password-hash");
        verify(customerRepository).saveAndFlush(customer);
        verify(refreshTokenRepository).deleteByUserEmail(customer.getEmail());
    }

    @Test
    void rejectsPasswordExceedingBcryptByteLimit() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.resetCustomerPassword(customer.getId(), new ResetPasswordRequest("\u20ac".repeat(30), 3L)))
                .hasMessageContaining("72 byte");
        verifyNoInteractions(passwordEncoder);
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnUnchangedPassword() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("SamePass123!", "stored-hash")).thenReturn(true);
        assertThatThrownBy(() -> service.resetCustomerPassword(customer.getId(), new ResetPasswordRequest("SamePass123!", 3L)))
                .hasMessageContaining("khác mật khẩu hiện tại");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInvalidStatusFilters() {
        assertThatThrownBy(() -> service.listCustomers("", "DELETED", 0, 10)).hasMessageContaining("trạng thái");
        verifyNoInteractions(queries);
    }

    @Test
    void updatesStaffWithoutResettingPasswordAndRevokesThePreviousLogin() {
        prepareStaff();
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        when(staffRepository.saveAndFlush(target)).thenReturn(target);
        UserAccount result = service.updateStaff(target.getId(), new UpdateStaffRequest(details("updated@example.com", List.of()), 2L));
        assertThat(result.email()).isEqualTo("updated@example.com");
        assertThat(result.staffCode()).isEqualTo("NV_NEW");
        assertThat(target.getPassword()).isEqualTo("stored-hash");
        assertThat(target.getStatus()).isEqualTo("ACTIVE");
        verify(refreshTokenRepository).deleteByUserEmail("staff@example.com");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsDemotingTheLastActiveAdministrator() {
        target.setRole(actor.getRole());
        prepareStaff();
        when(queries.lockActiveAdministrators()).thenReturn(List.of(target));
        assertThatThrownBy(() -> service.updateStaff(target.getId(), new UpdateStaffRequest(details("staff@example.com", List.of()), 2L)))
                .hasMessageContaining("ít nhất một Admin");
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMutationsByAnInactiveAdministrator() {
        actor.setStatus("LOCKED");
        prepareStaff();
        assertThatThrownBy(() -> service.changeStaffStatus(target.getId(), new ChangeStatusRequest("LOCKED", 2L)))
                .isInstanceOfSatisfying(CustomException.class, exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesUnknownAccountStatusWithoutWriting() {
        prepareStaff();
        assertThatThrownBy(() -> service.changeStaffStatus(target.getId(), new ChangeStatusRequest("DELETED", 2L)))
                .hasMessageContaining("ACTIVE hoặc LOCKED");
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void resetsStaffPasswordAndRevokesSessions() {
        prepareStaff();
        when(passwordEncoder.encode("BrandNew123!")).thenReturn("new-password-hash");
        service.resetStaffPassword(target.getId(), new ResetPasswordRequest("BrandNew123!", 2L));
        assertThat(target.getPassword()).isEqualTo("new-password-hash");
        verify(staffRepository).saveAndFlush(target);
        verify(refreshTokenRepository).deleteByUserEmail("staff@example.com");
    }

    @Test
    void optionsOnlyExposeAssignableStaffRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(staffRole, actor.getRole(), Role.builder().id(3L).name("ROLE_CUSTOMER").build()));
        assertThat(service.options().roles()).extracting(RoleOption::name).containsExactly("ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    void customerDetailsDoNotContainPasswordOrStaffPermissions() throws Exception {
        customer.setAddress("TP.HCM");
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        UserAccount result = service.getCustomer(customer.getId());
        assertThat(result.address()).isEqualTo("TP.HCM");
        assertThat(result.effectivePermissions()).isEmpty();
        assertThat(result.role()).isNull();
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(result);
        assertThat(json).doesNotContain("password", "stored-hash");
    }

    @Test
    void reportsNotFoundWithoutWriting() {
        assertThatThrownBy(() -> service.getStaff(UUID.randomUUID()))
                .isInstanceOfSatisfying(CustomException.class, exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(staffRepository, never()).saveAndFlush(any());
    }

    @Test
    void redactsPasswordRequestToString() {
        assertThat(new CreateStaffRequest(details("new@example.com", List.of()), "Sensitive123!").toString()).doesNotContain("Sensitive123!");
        assertThat(new ResetPasswordRequest("Sensitive123!", 0L).toString()).doesNotContain("Sensitive123!");
    }

    private void prepareStaff() {
        when(staffRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(currentUserService.getCurrentStaff()).thenReturn(actor);
    }

    private StaffDetails details(String email, List<String> permissions) {
        return new StaffDetails("  Nhân viên mới  ", email, null, null, "nv_new", LocalDate.now(),
                "Điều phối", "Bến xe", 2L, permissions);
    }
}
