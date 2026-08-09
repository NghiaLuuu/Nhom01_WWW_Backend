package vn.edu.iuh.nhom01_www.user.service;

import vn.edu.iuh.nhom01_www.user.dto.JwtResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.RegisterRequest;
import vn.edu.iuh.nhom01_www.user.dto.TokenRefreshRequest;
import vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest;

public interface AuthService {
    Object register(RegisterRequest request);
    Object verifyOtp(VerifyOtpRequest request);
    JwtResponse login(LoginRequest request);
    JwtResponse refreshToken(TokenRefreshRequest request);
}
