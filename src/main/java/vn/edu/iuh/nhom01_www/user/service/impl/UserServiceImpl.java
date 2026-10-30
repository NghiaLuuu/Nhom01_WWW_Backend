package vn.edu.iuh.nhom01_www.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.route.dto.RouteRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.RegisterRequest;
import vn.edu.iuh.nhom01_www.user.dto.StaffRequest;
import vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.user.service.UserService;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final vn.edu.iuh.nhom01_www.role.repository.PermissionRepository permissionRepository;
    private final vn.edu.iuh.nhom01_www.payment.service.RevenueReportService revenueReportService;

    @Override
    public Object register(RegisterRequest request) { return null; }

    @Override
    public Object verifyOtp(VerifyOtpRequest request) { return null; }

    @Override
    public String login(LoginRequest request) { return "jwt-token"; }

    @Override
    public List<Staff> getAllStaffs() {
        return staffRepository.findAll();
    }

    @Override
    @Transactional
    public Staff createStaff(StaffRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getFullName() == null || request.getFullName().isBlank()) {
            throw new CustomException("Email and fullName are required", HttpStatus.BAD_REQUEST);
        }
        validateStaffIdentity(null, request);
        validatePermissions(request.getPermissions());
        validatePassword(request.getPassword());
        Role role = null;
        if (request.getRoleId() != null) {
            role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));
        }
        if (role == null) {
            role = roleRepository.findByName("ROLE_STAFF")
                    .orElseThrow(() -> new CustomException("Default staff role is not configured", HttpStatus.CONFLICT));
        }
        validateStaffRole(role);

        String staffCode = request.getStaffCode();
        if (staffCode == null || staffCode.trim().isEmpty()) {
            staffCode = "NV" + UUID.randomUUID().toString().replace("-", "");
        }

        Staff staff = Staff.builder()
                .email(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT))
                .fullName(request.getFullName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .staffCode(staffCode)
                .position(request.getPosition())
                .workStation(request.getWorkStation())
                .role(role)
                .permissions(request.getPermissions() != null ? request.getPermissions() : new java.util.ArrayList<>())
                .hireDate(LocalDate.now())
                .status("ACTIVE")
                .build();
        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff updateStaff(UUID id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        validateStaffIdentity(id, request);
        validatePermissions(request.getPermissions());

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));
            validateStaffRole(role);
            if (isCurrentUser(staff) && staff.getRole() != null && !staff.getRole().getId().equals(role.getId())) {
                throw new CustomException("Cannot change your own role", HttpStatus.CONFLICT);
            }
            staff.setRole(role);
        }
        if (request.getEmail() != null) staff.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        if (request.getFullName() != null) {
            if (request.getFullName().isBlank()) throw new CustomException("fullName must not be blank", HttpStatus.BAD_REQUEST);
            staff.setFullName(request.getFullName().trim());
        }
        if (request.getStaffCode() != null) staff.setStaffCode(request.getStaffCode());
        if (request.getPhoneNumber() != null) staff.setPhoneNumber(request.getPhoneNumber());
        if (request.getDateOfBirth() != null) staff.setDateOfBirth(request.getDateOfBirth());
        if (request.getPosition() != null) staff.setPosition(request.getPosition());
        if (request.getWorkStation() != null) staff.setWorkStation(request.getWorkStation());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            validatePassword(request.getPassword());
            staff.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getPermissions() != null) {
            staff.setPermissions(request.getPermissions());
        }
        
        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff updateStaffStatus(UUID id, String status) {
        validateStatus(status);
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        if ("LOCKED".equals(status) && isCurrentUser(staff)) {
            throw new CustomException("Cannot lock your own account", HttpStatus.CONFLICT);
        }
        staff.setStatus(status);
        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff resetStaffPassword(UUID id, String newPassword) {
        validatePassword(newPassword);
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        staff.setPassword(passwordEncoder.encode(newPassword));
        return staffRepository.save(staff);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional
    public Customer updateCustomerStatus(UUID id, String status) {
        validateStatus(status);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));
        customer.setStatus(status);
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer resetCustomerPassword(UUID id, String newPassword) {
        validatePassword(newPassword);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));
        customer.setPassword(passwordEncoder.encode(newPassword));
        return customerRepository.save(customer);
    }

    @Override
    public Object getProfile(String email) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) return customer;
        Staff staff = staffRepository.findByEmail(email);
        if (staff != null) return staff;
        throw new CustomException("Profile not found", HttpStatus.NOT_FOUND);
    }

    @Override
    @Transactional
    public Object updateProfile(String email, vn.edu.iuh.nhom01_www.user.dto.UpdateProfileRequest request) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            customerRepository.updateProfileByEmail(
                email, 
                request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : null,
                request.getPhoneNumber(),
                request.getDateOfBirth(),
                request.getAddress()
            );
            return customerRepository.findByEmail(email);
        }

        Staff staff = staffRepository.findByEmail(email);
        if (staff != null) {
            staffRepository.updateProfileByEmail(
                email,
                request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : null,
                request.getPhoneNumber(),
                request.getDateOfBirth()
            );
            return staffRepository.findByEmail(email);
        }

        throw new CustomException("Profile not found", HttpStatus.NOT_FOUND);
    }

    @Override
    @Transactional
    public Object updatePassword(String email, vn.edu.iuh.nhom01_www.user.dto.UpdatePasswordRequest request) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), customer.getPassword())) {
                throw new CustomException("Old password does not match", HttpStatus.BAD_REQUEST);
            }
            customerRepository.updatePasswordByEmail(email, passwordEncoder.encode(request.getNewPassword()));
            return customerRepository.findByEmail(email);
        }

        Staff staff = staffRepository.findByEmail(email);
        if (staff != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), staff.getPassword())) {
                throw new CustomException("Old password does not match", HttpStatus.BAD_REQUEST);
            }
            staffRepository.updatePasswordByEmail(email, passwordEncoder.encode(request.getNewPassword()));
            return staffRepository.findByEmail(email);
        }

        throw new CustomException("User not found", HttpStatus.NOT_FOUND);
    }

    @Override
    public Object getRevenueStatistics() {
        return revenueReportService.report(LocalDate.now().withDayOfMonth(1), LocalDate.now(),
                vn.edu.iuh.nhom01_www.payment.service.RevenueReportService.GroupBy.DAY);
    }

    private void validateStatus(String status) {
        if (!"ACTIVE".equals(status) && !"LOCKED".equals(status)) {
            throw new CustomException("Status must be ACTIVE or LOCKED", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new CustomException("Password must contain at least 8 characters and at most 72 UTF-8 bytes", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isCurrentUser(Staff staff) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && staff.getEmail().equalsIgnoreCase(authentication.getName());
    }

    private void validateStaffRole(Role role) {
        if (!java.util.Set.of("ROLE_ADMIN", "ROLE_STAFF").contains(role.getName())) {
            throw new CustomException("A staff account must have ROLE_STAFF or ROLE_ADMIN", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePermissions(List<String> permissions) {
        if (permissions == null) return;
        for (String permission : permissions) {
            if (permission == null || permissionRepository.findByCode(permission).isEmpty()) {
                throw new CustomException("Unknown permission", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateStaffIdentity(UUID id, StaffRequest request) {
        if (request.getEmail() != null) {
            if (request.getEmail().isBlank()) throw new CustomException("Email is required", HttpStatus.BAD_REQUEST);
            Staff existing = staffRepository.findByEmailIgnoreCase(request.getEmail().trim());
            if (existing != null && !existing.getId().equals(id)) throw new CustomException("Email already in use", HttpStatus.CONFLICT);
            if (customerRepository.findByEmailIgnoreCase(request.getEmail().trim()) != null) {
                throw new CustomException("Email belongs to a customer account", HttpStatus.CONFLICT);
            }
        }
        if (request.getStaffCode() != null && !request.getStaffCode().isBlank()) {
            Staff existing = staffRepository.findByStaffCodeIgnoreCase(request.getStaffCode());
            if (existing != null && !existing.getId().equals(id)) throw new CustomException("Staff code already in use", HttpStatus.CONFLICT);
        }
        if (request.getPhoneNumber() != null) {
            staffRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) throw new CustomException("Phone number already in use", HttpStatus.CONFLICT);
            });
        }
    }

    // --- OTHER STUBS ---
    public List<Route> getAllRoutes() { return Collections.emptyList(); }
    public Route getRouteById(Long id) { return null; }
    public Route createRoute(RouteRequest request) { return null; }
    public Route updateRoute(Long id, RouteRequest request) { return null; }
    public void deleteRoute(Long id) {}

    public List<Vehicle> getAllVehicles() { return Collections.emptyList(); }
    public Vehicle createVehicle(VehicleRequest request) { return null; }
    public Vehicle updateVehicle(Long id, VehicleRequest request) { return null; }
    public void deleteVehicle(Long id) {}

    public Object ask(ChatbotRequest request) { return null; }
}
