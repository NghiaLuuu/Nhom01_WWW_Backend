package vn.edu.iuh.nhom01_www.user.service;

public interface UserService {
    Object register(vn.edu.iuh.nhom01_www.user.dto.RegisterRequest req);
    Object verifyOtp(vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest req);
    String login(vn.edu.iuh.nhom01_www.user.dto.LoginRequest req);
    java.util.List<vn.edu.iuh.nhom01_www.user.model.Staff> getAllStaffs();
    java.util.List<vn.edu.iuh.nhom01_www.user.model.Customer> getAllCustomers();
    Object getRevenueStatistics();
}
