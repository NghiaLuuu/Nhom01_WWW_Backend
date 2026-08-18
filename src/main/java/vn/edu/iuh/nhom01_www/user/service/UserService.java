package vn.edu.iuh.nhom01_www.user.service;

import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.route.dto.RouteRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.RegisterRequest;
import vn.edu.iuh.nhom01_www.user.dto.StaffRequest;
import vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.util.List;
import java.util.UUID;

public interface UserService {
    Object register(RegisterRequest request);
    Object verifyOtp(VerifyOtpRequest request);
    String login(LoginRequest request);

    List<Staff> getAllStaffs();
    Staff createStaff(StaffRequest request);
    Staff updateStaff(UUID id, StaffRequest request);
    Staff updateStaffStatus(UUID id, String status);
    Staff resetStaffPassword(UUID id);

    List<Customer> getAllCustomers();
    Customer updateCustomerStatus(UUID id, String status);
    Customer resetCustomerPassword(UUID id);

    Object getProfile(String email);
    Object updateProfile(String email, vn.edu.iuh.nhom01_www.user.dto.UpdateProfileRequest request);
    Object updatePassword(String email, vn.edu.iuh.nhom01_www.user.dto.UpdatePasswordRequest request);

    Object getRevenueStatistics();

    List<Route> getAllRoutes();
    Route getRouteById(Long id);
    Route createRoute(RouteRequest request);
    Route updateRoute(Long id, RouteRequest request);
    void deleteRoute(Long id);

    List<Vehicle> getAllVehicles();
    Vehicle createVehicle(VehicleRequest request);
    Vehicle updateVehicle(Long id, VehicleRequest request);
    void deleteVehicle(Long id);

    Object ask(ChatbotRequest request);
}
