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
        if (staffRepository.findByEmail(request.getEmail()) != null) {
            throw new CustomException("Email already in use", HttpStatus.BAD_REQUEST);
        }
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));

        Staff staff = Staff.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode("Vexe@123"))
                .staffCode(request.getStaffCode())
                .position(request.getPosition())
                .workStation(request.getWorkStation())
                .role(role)
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
        
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));
            staff.setRole(role);
        }
        
        if (request.getFullName() != null) staff.setFullName(request.getFullName());
        if (request.getPosition() != null) staff.setPosition(request.getPosition());
        if (request.getWorkStation() != null) staff.setWorkStation(request.getWorkStation());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            staff.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff updateStaffStatus(UUID id, String status) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        staff.setStatus(status);
        return staffRepository.save(staff);
    }

    @Override
    @Transactional
    public Staff resetStaffPassword(UUID id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));
        staff.setPassword(passwordEncoder.encode("Vexe@123"));
        return staffRepository.save(staff);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional
    public Customer updateCustomerStatus(UUID id, String status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));
        customer.setStatus(status);
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
    public Object updatePassword(String email, vn.edu.iuh.nhom01_www.user.dto.UpdatePasswordRequest request) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), customer.getPassword())) {
                throw new CustomException("Old password does not match", HttpStatus.BAD_REQUEST);
            }
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
            return customerRepository.save(customer);
        }

        Staff staff = staffRepository.findByEmail(email);
        if (staff != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), staff.getPassword())) {
                throw new CustomException("Old password does not match", HttpStatus.BAD_REQUEST);
            }
            staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
            return staffRepository.save(staff);
        }

        throw new CustomException("User not found", HttpStatus.NOT_FOUND);
    }

    @Override
    public Object getRevenueStatistics() {
        return Collections.singletonMap("message", "Revenue statistics not fully implemented yet");
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
