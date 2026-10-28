package vn.edu.iuh.nhom01_www.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.user.dto.JwtResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginApiResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.RegisterRequest;
import vn.edu.iuh.nhom01_www.user.dto.TokenRefreshRequest;
import vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest;
import vn.edu.iuh.nhom01_www.user.service.AuthService;
import vn.edu.iuh.nhom01_www.common.audit.annotation.Auditable;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Auditable(action = "REGISTER", entityName = "Auth")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest request) {
        Object response = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful. Please verify OTP.", response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        Object response = authService.verifyOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account verified successfully", response));
    }

    @PostMapping("/login")
    @Auditable(action = "LOGIN", entityName = "Auth")
    public ResponseEntity<LoginApiResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(LoginApiResponse.builder()
                .success(true)
                .message("Login successful")
                .token(response.getAccessToken())
                .user(response.getUser())
                .data(response)
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        JwtResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refreshed successfully", response));
    }
    @PostMapping("/forgot-password")
    @Auditable(action = "FORGOT_PASSWORD", entityName = "Auth")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody vn.edu.iuh.nhom01_www.user.dto.ForgotPasswordRequest request) {
        Object response = authService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset initiated", response));
    }

    @PostMapping("/reset-password")
    @Auditable(action = "RESET_PASSWORD", entityName = "Auth")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody vn.edu.iuh.nhom01_www.user.dto.ResetPasswordRequest request) {
        Object response = authService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", response));
    }
}
