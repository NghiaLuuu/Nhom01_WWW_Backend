package vn.edu.iuh.nhom01_www.user.service.impl;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.user.service.UserService;
@Service
public class UserServiceImpl implements UserService {
    // Stubs
    public Object register(vn.edu.iuh.nhom01_www.user.dto.RegisterRequest req) { return null; }
    public Object verifyOtp(vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest req) { return null; }
    public String login(vn.edu.iuh.nhom01_www.user.dto.LoginRequest req) { return "jwt-token"; }
    public java.util.List<vn.edu.iuh.nhom01_www.user.model.Staff> getAllStaffs() { return java.util.Collections.emptyList(); }
    public java.util.List<vn.edu.iuh.nhom01_www.user.model.Customer> getAllCustomers() { return java.util.Collections.emptyList(); }
    public Object getRevenueStatistics() { return null; }
    public java.util.List<vn.edu.iuh.nhom01_www.route.model.Route> getAllRoutes() { return java.util.Collections.emptyList(); }
    public vn.edu.iuh.nhom01_www.route.model.Route getRouteById(Long id) { return null; }
    public vn.edu.iuh.nhom01_www.route.model.Route createRoute(vn.edu.iuh.nhom01_www.route.dto.RouteRequest req) { return null; }
    public vn.edu.iuh.nhom01_www.route.model.Route updateRoute(Long id, vn.edu.iuh.nhom01_www.route.dto.RouteRequest req) { return null; }
    public void deleteRoute(Long id) {}
    public java.util.List<vn.edu.iuh.nhom01_www.vehicle.model.Vehicle> getAllVehicles() { return java.util.Collections.emptyList(); }
    public vn.edu.iuh.nhom01_www.vehicle.model.Vehicle createVehicle(vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest req) { return null; }
    public vn.edu.iuh.nhom01_www.vehicle.model.Vehicle updateVehicle(Long id, vn.edu.iuh.nhom01_www.vehicle.dto.VehicleRequest req) { return null; }
    public void deleteVehicle(Long id) {}
    public Object ask(vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest req) { return null; }
}
