package vn.edu.iuh.nhom01_www.user.adminmanagement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.repository.RefreshTokenRepository;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.user.adminmanagement.dto.UserManagementDtos.*;
import vn.edu.iuh.nhom01_www.user.adminmanagement.repository.UserManagementQueries;
import vn.edu.iuh.nhom01_www.user.model.BaseUser;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminUserManagementService {
    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_STAFF");
    private static final Set<String> FILTER_STATUSES = Set.of("ACTIVE", "LOCKED", "INACTIVE", "PENDING_VERIFICATION");

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserManagementQueries queries;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserService currentUserService;

    public AdminUserManagementService(StaffRepository staffRepository, CustomerRepository customerRepository,
            RoleRepository roleRepository, PermissionRepository permissionRepository, UserManagementQueries queries,
            PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository,
            CurrentUserService currentUserService) {
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.queries = queries;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.currentUserService = currentUserService;
    }

    public PageResult<UserAccount> listStaff(String keyword, String status, Long roleId, int page, int size) {
        Page<Staff> rows = queries.findStaff(keyword, filterStatus(status), roleId, PageRequest.of(page, size));
        return page(rows.map(this::staffAccount));
    }

    public PageResult<UserAccount> listCustomers(String keyword, String status, int page, int size) {
        Page<Customer> rows = queries.findCustomers(keyword, filterStatus(status), PageRequest.of(page, size));
        return page(rows.map(this::customerAccount));
    }

    public UserAccount getStaff(UUID id) {
        return staffAccount(staff(id));
    }

    public UserAccount getCustomer(UUID id) {
        return customerAccount(customer(id));
    }

    public Options options() {
        List<RoleOption> roles = roleRepository.findAll().stream()
                .filter(role -> STAFF_ROLES.contains(role.getName()))
                .map(this::roleOption).sorted(java.util.Comparator.comparing(RoleOption::name)).toList();
        List<PermissionOption> permissions = permissionRepository.findAll().stream()
                .map(p -> new PermissionOption(p.getId(), p.getCode(), p.getDescription()))
                .sorted(java.util.Comparator.comparing(PermissionOption::code)).toList();
        return new Options(roles, permissions);
    }

    @Transactional
    public UserAccount createStaff(CreateStaffRequest request) {
        Staff user = new Staff();
        applyDetails(user, request.details(), null);
        user.setPassword(encodePassword(request.password()));
        user.setStatus("ACTIVE");
        return staffAccount(staffRepository.saveAndFlush(user));
    }

    @Transactional
    public UserAccount updateStaff(UUID id, UpdateStaffRequest request) {
        Staff user = staff(id);
        ensureOtherStaff(user);
        ensureVersion(user, request.version());
        if (isAdministrator(user) && !Objects.equals(user.getRole().getId(), request.details().roleId())) {
            ensureAdministratorRemains(user);
        }
        String previousEmail = user.getEmail();
        applyDetails(user, request.details(), id);
        Staff saved = staffRepository.saveAndFlush(user);
        refreshTokenRepository.deleteByUserEmail(previousEmail);
        return staffAccount(saved);
    }

    @Transactional
    public UserAccount changeStaffStatus(UUID id, ChangeStatusRequest request) {
        Staff user = staff(id);
        ensureOtherStaff(user);
        ensureVersion(user, request.version());
        ensureStatus(request.status());
        if (Objects.equals(user.getStatus(), request.status())) {
            return staffAccount(user);
        }
        if (isAdministrator(user) && "ACTIVE".equals(user.getStatus()) && "LOCKED".equals(request.status())) {
            ensureAdministratorRemains(user);
        }
        user.setStatus(request.status());
        Staff saved = staffRepository.saveAndFlush(user);
        refreshTokenRepository.deleteByUserEmail(user.getEmail());
        return staffAccount(saved);
    }

    @Transactional
    public UserAccount changeCustomerStatus(UUID id, ChangeStatusRequest request) {
        Customer user = customer(id);
        ensureVersion(user, request.version());
        ensureStatus(request.status());
        if ("PENDING_VERIFICATION".equals(user.getStatus())) {
            throw error("Tài khoản này chưa xác thực OTP; không thể kích hoạt từ màn quản trị.", HttpStatus.CONFLICT);
        }
        if (Objects.equals(user.getStatus(), request.status())) {
            return customerAccount(user);
        }
        user.setStatus(request.status());
        Customer saved = customerRepository.saveAndFlush(user);
        refreshTokenRepository.deleteByUserEmail(user.getEmail());
        return customerAccount(saved);
    }

    @Transactional
    public void resetStaffPassword(UUID id, ResetPasswordRequest request) {
        Staff user = staff(id);
        ensureOtherStaff(user);
        resetPassword(user, request);
        staffRepository.saveAndFlush(user);
        refreshTokenRepository.deleteByUserEmail(user.getEmail());
    }

    @Transactional
    public void resetCustomerPassword(UUID id, ResetPasswordRequest request) {
        Customer user = customer(id);
        resetPassword(user, request);
        customerRepository.saveAndFlush(user);
        refreshTokenRepository.deleteByUserEmail(user.getEmail());
    }

    private void resetPassword(BaseUser user, ResetPasswordRequest request) {
        ensureVersion(user, request.version());
        String encodedPassword = encodePassword(request.password());
        if (passwordEncoder.matches(request.password(), user.getPassword())) {
            throw error("Mật khẩu mới phải khác mật khẩu hiện tại.", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(encodedPassword);
    }

    private void applyDetails(Staff user, StaffDetails details, UUID excludedId) {
        String email = details.email().trim().toLowerCase(Locale.ROOT);
        String code = details.staffCode().trim().toUpperCase(Locale.ROOT);
        String phone = optional(details.phoneNumber());
        if (queries.emailInUse(email, excludedId)) {
            throw error("Email đã được sử dụng bởi nhân viên hoặc khách hàng.", HttpStatus.CONFLICT);
        }
        if (queries.staffCodeInUse(code, excludedId)) {
            throw error("Mã nhân viên đã tồn tại.", HttpStatus.CONFLICT);
        }
        if (queries.staffPhoneInUse(phone, excludedId)) {
            throw error("Số điện thoại đã được sử dụng bởi nhân viên khác.", HttpStatus.CONFLICT);
        }
        Role role = roleRepository.findById(details.roleId())
                .orElseThrow(() -> error("Vai trò không tồn tại.", HttpStatus.BAD_REQUEST));
        if (!STAFF_ROLES.contains(role.getName())) {
            throw error("Nhân viên chỉ được gán vai trò Admin hoặc Staff.", HttpStatus.BAD_REQUEST);
        }
        Set<String> knownPermissions = new TreeSet<>();
        permissionRepository.findAll().forEach(p -> knownPermissions.add(p.getCode()));
        TreeSet<String> extraPermissions = new TreeSet<>(details.permissions());
        if (!knownPermissions.containsAll(extraPermissions)) {
            throw error("Có quyền không tồn tại trong hệ thống.", HttpStatus.BAD_REQUEST);
        }
        extraPermissions.removeAll(rolePermissions(role));
        user.setFullName(details.fullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setDateOfBirth(details.dateOfBirth());
        user.setStaffCode(code);
        user.setHireDate(details.hireDate() == null ? LocalDate.now() : details.hireDate());
        user.setPosition(optional(details.position()));
        user.setWorkStation(optional(details.workStation()));
        user.setRole(role);
        user.setPermissions(new ArrayList<>(extraPermissions));
    }

    private String encodePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw error("Mật khẩu phải có ít nhất 8 ký tự và không vượt quá 72 byte UTF-8.", HttpStatus.BAD_REQUEST);
        }
        return passwordEncoder.encode(password);
    }

    private Staff staff(UUID id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> error("Không tìm thấy nhân viên.", HttpStatus.NOT_FOUND));
    }

    private Customer customer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> error("Không tìm thấy khách hàng.", HttpStatus.NOT_FOUND));
    }

    private void ensureOtherStaff(Staff target) {
        Staff actor = currentUserService.getCurrentStaff();
        if (!"ACTIVE".equals(actor.getStatus())) {
            throw error("Tài khoản quản trị không hoạt động.", HttpStatus.FORBIDDEN);
        }
        if (Objects.equals(actor.getId(), target.getId())) {
            throw error("Không thay đổi tài khoản đang đăng nhập tại đây. Hãy dùng trang Hồ sơ cá nhân.", HttpStatus.CONFLICT);
        }
    }

    private void ensureVersion(BaseUser user, Long version) {
        if (version == null || !Objects.equals(user.getVersion(), version)) {
            throw error("Dữ liệu đã thay đổi. Vui lòng tải lại danh sách trước khi thao tác.", HttpStatus.CONFLICT);
        }
    }

    private void ensureAdministratorRemains(Staff target) {
        List<Staff> administrators = queries.lockActiveAdministrators();
        if (administrators.stream().noneMatch(a -> !a.getId().equals(target.getId()))) {
            throw error("Phải giữ lại ít nhất một Admin đang hoạt động.", HttpStatus.CONFLICT);
        }
    }

    private boolean isAdministrator(Staff user) {
        return user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
    }

    private void ensureStatus(String status) {
        if (!Set.of("ACTIVE", "LOCKED").contains(status == null ? "" : status)) {
            throw error("Chỉ chấp nhận trạng thái ACTIVE hoặc LOCKED.", HttpStatus.BAD_REQUEST);
        }
    }

    private String filterStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!FILTER_STATUSES.contains(status)) {
            throw error("Bộ lọc trạng thái không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> rolePermissions(Role role) {
        return role == null || role.getPermissions() == null ? List.of()
                : role.getPermissions().stream().map(Permission::getCode).sorted().toList();
    }

    private RoleOption roleOption(Role role) {
        return new RoleOption(role.getId(), role.getName(), role.getDescription(), rolePermissions(role));
    }

    private UserAccount staffAccount(Staff user) {
        List<String> extra = user.getPermissions() == null ? List.of() : List.copyOf(user.getPermissions());
        Collection<String> effective = new TreeSet<>(rolePermissions(user.getRole()));
        effective.addAll(extra);
        return account(user, user.getStaffCode(), user.getHireDate(), user.getPosition(), user.getWorkStation(),
                null, user.getRole() == null ? null : roleOption(user.getRole()), extra, List.copyOf(effective));
    }

    private UserAccount customerAccount(Customer user) {
        return account(user, null, null, null, null, user.getAddress(), null, List.of(), List.of());
    }

    private UserAccount account(BaseUser user, String code, LocalDate hired, String position, String station,
            String address, RoleOption role, List<String> permissions, List<String> effective) {
        return new UserAccount(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                user.getDateOfBirth(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt(), user.getVersion(),
                code, hired, position, station, address, role, permissions, effective);
    }

    private <T> PageResult<T> page(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private CustomException error(String message, HttpStatus status) {
        return new CustomException(message, status);
    }
}
